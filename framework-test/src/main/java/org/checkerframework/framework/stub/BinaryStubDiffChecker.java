package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.plumelib.util.IPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * Test-only differential checker for the binary stub path. For every top-level class in the binary
 * stub data — of the annotated JDK ({@link #run}) and of each built-in checker stub file ({@link
 * #diffBuiltinStub}) — it loads the class's annotations twice, once through {@link
 * BinaryStubReader} and once through the text-based {@link AnnotationFileParser}, into separate
 * scratch {@link AnnotationFileAnnotations} containers, and reports any disagreement between the
 * two as an error.
 *
 * <p>Because the binary writer and reader re-implement the text parser's semantics (TYPE_USE
 * routing, array-component rules, type-parameter bounds, fake overrides, {@code @FromStubFile}
 * marking, ...), a silent divergence between the two paths is the main correctness risk of the
 * binary format. This check turns such a divergence into a loud test failure. It is activated by
 * the {@code -AbinaryStubDiffCheck} option (used by the {@code NullnessBinaryStubDiffTest} JUnit
 * test) and is never active in normal checker runs.
 *
 * <p>Comparison semantics:
 *
 * <ul>
 *   <li>Declaration annotations ({@link AnnotationFileAnnotations#declAnnos}) must have the same
 *       keys and, per key, the same set of Checker Framework annotation class names. Non-checker
 *       annotations (an unimported {@code @Override}, a {@code @DefinedBy} reachable only through a
 *       static import, {@code @CFComment}) are kept or dropped differently by well-understood
 *       text-parser name-resolution quirks, are consumed by no checker, and are excluded.
 *   <li>Annotated types ({@link AnnotationFileAnnotations#atypes}) are compared per element key by
 *       a parallel, cycle-safe structural walk that compares the set of annotation names at every
 *       source-annotatable type component (see {@link #compareAtm} for the derived positions that
 *       are excluded and why). An entry present on only one side is reported only if it carries
 *       non-derived annotations.
 *   <li>Fake overrides ({@link AnnotationFileAnnotations#fakeOverrides}) are compared by which
 *       (overridden method, subtype) entries exist, in both directions -- not by the stored types,
 *       whose baselines differ between the two loaders. Both directions are needed: the binary path
 *       resolves a fake override by a signature lookup and the text path by matching parameters
 *       structurally, so either can miss one the other applies. {@link #verifyRecordApplied} adds a
 *       record oracle on top, asserting that every binary record -- including a presence-only
 *       signature -- that matches an inherited method did produce an entry.
 *   <li>A type variable's <em>primary</em> annotation -- the shape written {@code <@Nullable T>} --
 *       is compared by the structural walk wherever it appears, including on the type arguments of
 *       the declared type stored for a {@code TypeElement}.
 *   <li>A type variable's <em>bounds</em> -- the shape written {@code <T extends @Nullable
 *       Object>}, as on {@code StackWalker.walk} -- are covered by {@link
 *       #verifyTypeParamAnnosApplied}, an oracle against the binary record, rather than by the
 *       structural walk. The walk cannot compare them: the two loaders' bounds have different
 *       baselines, so a direct comparison reports defaults as mismatches (the text parser stores
 *       {@code java.util.List}'s {@code E} with its upper bound already defaulted to
 *       {@code @Initialized @NonNull @UnknownKeyFor}, while the binary reader stores only what the
 *       stub wrote and lets the framework default the rest later). Asking instead whether
 *       everything the record promises was applied is immune to that, since defaulting only ever
 *       adds annotations.
 *   <li>Record stubs ({@link AnnotationFileParser.AnnotationFileAnnotations#records}) are compared
 *       structurally: same class keys and same component names in the same order. Annotation
 *       agreement is covered by the atypes and declAnnos checks above.
 * </ul>
 */
public class BinaryStubDiffChecker {

    /** Do not instantiate; all methods are static. */
    private BinaryStubDiffChecker() {}

    /** Maximum number of detailed mismatch messages to report before summarizing only. */
    private static final int MAX_DETAILED_REPORTS = 100;

    /**
     * Runs the differential check over every top-level class in the binary stub data and reports
     * mismatches as errors via the checker.
     *
     * @param elementTypes the stub-types AFET whose factory and stub sources to use
     * @param cache the per-compilation binary stub cache; its data supplies the classes to check
     * @param atypeFactory the factory used to create types and parse annotations
     */
    public static void run(
            AnnotationFileElementTypes elementTypes,
            AnnotationFileElementTypes.BinaryStubDataCache cache,
            AnnotatedTypeFactory atypeFactory) {
        BinaryStubData data = cache.data;
        SourceChecker checker = atypeFactory.getChecker();

        int classesChecked = 0;
        int classesWithoutTextStub = 0;
        List<String> reports = new ArrayList<>();

        for (Map.Entry<String, BinaryStubData.ClassRecord> entry : data.classes.entrySet()) {
            BinaryStubData.ClassRecord cr = entry.getValue();
            if (cr.outerNameIndex != 0) {
                // Inner classes are checked together with their outermost class, mirroring the
                // text parser, which always parses a whole .java file at once.
                continue;
            }
            String className = entry.getKey();

            AnnotationFileAnnotations textAnnos = new AnnotationFileAnnotations();
            if (!elementTypes.parseJdkSourceInto(className, textAnnos)) {
                // Auxiliary (package-private) top-level classes live in the .java file of
                // another class and have no file of their own; they are checked as part of
                // that hosting file's comparison below.
                classesWithoutTextStub++;
                continue;
            }

            // The text parser processes a whole .java file at once, which may contain
            // auxiliary (package-private) top-level classes besides the named one. Apply the
            // binary records at the same granularity: the named class, plus every top-level
            // class the text side produced results for, plus all their inner classes.
            AnnotationFileAnnotations binaryAnnos = new AnnotationFileAnnotations();
            Set<String> appliedTops = new HashSet<>();
            List<BinaryStubData.ClassRecord> appliedRecords = new ArrayList<>();
            applyTopLevelRecord(
                    className,
                    cr,
                    appliedTops,
                    appliedRecords,
                    binaryAnnos,
                    elementTypes,
                    data,
                    atypeFactory);
            for (String top : topLevelClassesIn(textAnnos, data)) {
                BinaryStubData.ClassRecord topCr = data.classes.get(top);
                if (topCr != null && topCr.outerNameIndex == 0) {
                    applyTopLevelRecord(
                            top,
                            topCr,
                            appliedTops,
                            appliedRecords,
                            binaryAnnos,
                            elementTypes,
                            data,
                            atypeFactory);
                }
            }

            classesChecked++;
            for (BinaryStubData.ClassRecord appliedCr : appliedRecords) {
                verifyRecordApplied(
                        data.stringPool[appliedCr.nameIndex],
                        appliedCr,
                        data,
                        binaryAnnos,
                        elementTypes,
                        atypeFactory,
                        reports);
            }
            compareClass(className, textAnnos, binaryAnnos, atypeFactory, reports);
        }

        comparePackagesAndModules(elementTypes, data, atypeFactory, reports);

        reportDiffs(checker, reports);
        // Print the summary to standard output rather than as a NOTE diagnostic: the
        // per-directory test harness treats every diagnostic as unexpected.
        System.out.printf(
                "binary stub diff: checked %d top-level classes (%d without text stub source),"
                        + " %d mismatches.%n",
                classesChecked, classesWithoutTextStub, reports.size());
    }

    /**
     * Differential check for package- and module-level declaration annotations: for every package
     * and module the binary stub data gives annotations to ({@link BinaryStubData#packages}, {@link
     * BinaryStubData#modules}), text-parses its {@code package-info.java}/{@code module-info.java}
     * and compares the resulting declaration annotations against what {@link
     * BinaryStubReader#applyPackageAndModuleRecords} produces from the binary record, reporting any
     * disagreement. Unlike the per-class comparison in {@link #run}, this is a single pass over all
     * packages and modules rather than one per class, since {@link BinaryStubData#packages}/{@link
     * BinaryStubData#modules} are already complete, independent maps (not something the class loop
     * discovers incrementally).
     *
     * @param elementTypes the stub-types AFET whose factory to use
     * @param data the complete binary stub data
     * @param atypeFactory the factory used to create types and parse annotations
     * @param reports the list of mismatch descriptions to append to
     */
    private static void comparePackagesAndModules(
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            List<String> reports) {
        AnnotationFileAnnotations textAnnos = new AnnotationFileAnnotations();
        for (String packageName : data.packages.keySet()) {
            elementTypes.parseJdkPackageInfoInto(packageName, textAnnos);
        }
        for (String moduleName : data.modules.keySet()) {
            elementTypes.parseJdkModuleInfoInto(moduleName, textAnnos);
        }
        AnnotationFileAnnotations binaryAnnos = new AnnotationFileAnnotations();
        BinaryStubReader.applyPackageAndModuleRecords(
                data, atypeFactory, elementTypes, binaryAnnos, /* fromLazyJdk= */ true);

        compareAnnotations(
                "package",
                data.packages,
                textAnnos.declAnnos,
                binaryAnnos.declAnnos,
                atypeFactory,
                reports);
        compareAnnotations(
                "module",
                data.modules,
                textAnnos.declAnnos,
                binaryAnnos.declAnnos,
                atypeFactory,
                reports);
    }

    /**
     * Compares declaration annotations for a set of items (like packages or modules) between
     * text-parsed and binary-loaded representations, appending mismatch descriptions to the given
     * reports list.
     *
     * @param typeName a human-readable description of the item type (e.g., "package" or "module")
     * @param items the items to compare, keyed by name; only the keys are used
     * @param textAnnos the annotations produced by the text parser
     * @param binaryAnnos the annotations produced by the binary reader
     * @param atypeFactory the factory, for recognizing aliased annotations
     * @param reports the list to append mismatch descriptions to
     */
    private static void compareAnnotations(
            String typeName,
            Map<String, ?> items,
            Map<String, AnnotationMirrorSet> textAnnos,
            Map<String, AnnotationMirrorSet> binaryAnnos,
            AnnotatedTypeFactory atypeFactory,
            List<String> reports) {
        for (String name : items.keySet()) {
            Set<String> textNames = comparedAnnotationNames(textAnnos.get(name), atypeFactory);
            Set<String> binaryNames = comparedAnnotationNames(binaryAnnos.get(name), atypeFactory);
            if (!textNames.equals(binaryNames)) {
                reports.add(
                        String.format(
                                "%s %s: declAnnos: text=%s binary=%s",
                                typeName, name, textNames, binaryNames));
            }
        }
    }

    /**
     * Reports each mismatch as an error, up to {@link #MAX_DETAILED_REPORTS}, then summarizes any
     * remainder.
     *
     * @param checker the checker to report through
     * @param reports the mismatch descriptions
     */
    private static void reportDiffs(SourceChecker checker, List<String> reports) {
        for (int i = 0; i < reports.size() && i < MAX_DETAILED_REPORTS; i++) {
            checker.message(Diagnostic.Kind.ERROR, "binary stub diff: " + reports.get(i));
        }
        if (reports.size() > MAX_DETAILED_REPORTS) {
            checker.message(
                    Diagnostic.Kind.ERROR,
                    "binary stub diff: "
                            + (reports.size() - MAX_DETAILED_REPORTS)
                            + " further mismatches suppressed.");
        }
    }

    /**
     * Differential check for one built-in checker stub file (e.g. {@code jdk.astub}): text-parses
     * the {@code .astub} resource and applies its binary form into separate scratch containers,
     * then reports any disagreement as an error, exactly like the annotated-JDK check in {@link
     * #run}. Both sides include {@code @FromStubFile} marking, so the marking parity is verified as
     * well.
     *
     * @param description resource description, used in report messages
     * @param textResourceURL the URL of the {@code .astub} resource
     * @param data the binary form of the stub file
     * @param elementTypes the stub-types AFET whose factory to use
     * @param atypeFactory the factory used to create types and parse annotations
     */
    public static void diffBuiltinStub(
            String description,
            java.net.URL textResourceURL,
            BinaryStubData data,
            AnnotationFileElementTypes elementTypes,
            AnnotatedTypeFactory atypeFactory) {
        SourceChecker checker = atypeFactory.getChecker();
        AnnotationFileAnnotations textAnnos = new AnnotationFileAnnotations();
        try (java.io.InputStream in = textResourceURL.openStream()) {
            AnnotationFileParser.parseStubFile(
                    description,
                    in,
                    atypeFactory,
                    atypeFactory.getProcessingEnv(),
                    textAnnos,
                    AnnotationFileUtil.AnnotationFileType.BUILTIN_STUB,
                    elementTypes);
        } catch (java.io.IOException e) {
            checker.message(
                    Diagnostic.Kind.ERROR,
                    "binary stub diff: cannot read " + textResourceURL + ": " + e.getMessage());
            return;
        }
        AnnotationFileAnnotations binaryAnnos = new AnnotationFileAnnotations();
        elementTypes.applyBinaryStubData(data, binaryAnnos);

        List<String> reports = new ArrayList<>();
        for (Map.Entry<String, BinaryStubData.ClassRecord> entry : data.classes.entrySet()) {
            verifyRecordApplied(
                    entry.getKey(),
                    entry.getValue(),
                    data,
                    binaryAnnos,
                    elementTypes,
                    atypeFactory,
                    reports);
        }
        compareClass(description, textAnnos, binaryAnnos, atypeFactory, reports);
        reportDiffs(checker, reports);
        System.out.printf(
                "binary stub diff: checked built-in stub %s, %d mismatches.%n",
                description, reports.size());
    }

    /**
     * Applies one top-level class record and all its inner-class records into {@code binaryAnnos},
     * recording what was applied. Does nothing if the class was already applied for this
     * comparison.
     *
     * @param className the fully-qualified name of the top-level class
     * @param cr its class record
     * @param appliedTops the top-level class names already applied for this comparison
     * @param appliedRecords collects every record applied, for {@link #verifyRecordApplied}
     * @param binaryAnnos the scratch container to apply into
     * @param elementTypes the stub-types AFET
     * @param data the binary stub data
     * @param atypeFactory the factory used to create types and parse annotations
     */
    private static void applyTopLevelRecord(
            String className,
            BinaryStubData.ClassRecord cr,
            Set<String> appliedTops,
            List<BinaryStubData.ClassRecord> appliedRecords,
            AnnotationFileAnnotations binaryAnnos,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory) {
        if (!appliedTops.add(className)) {
            return;
        }
        BinaryStubReader.applyClassRecord(
                cr, className, atypeFactory, elementTypes, data, binaryAnnos);
        appliedRecords.add(cr);
        for (BinaryStubData.ClassRecord innerCr :
                elementTypes.getInnerClassesFromBinary(className)) {
            String innerName = data.stringPool[innerCr.nameIndex];
            BinaryStubReader.applyClassRecord(
                    innerCr, innerName, atypeFactory, elementTypes, data, binaryAnnos);
            appliedRecords.add(innerCr);
        }
    }

    /**
     * Determines the top-level classes for which the text parse produced results, by examining the
     * keys of both {@code atypes} (elements, walked to their outermost enclosing type) and {@code
     * declAnnos} (qualified-name strings, matched against the binary class index by successively
     * shorter prefixes).
     *
     * @param textAnnos the text parser's results for one file
     * @param data the binary stub data, used to resolve a class to its outermost enclosing class
     * @return the fully-qualified names of the outermost classes with results
     */
    private static Set<String> topLevelClassesIn(
            AnnotationFileAnnotations textAnnos, BinaryStubData data) {
        Set<String> result = new HashSet<>();
        for (Element key : textAnnos.atypes.keySet()) {
            TypeElement outermost = ElementUtils.toplevelEnclosingTypeElement(key);
            result.add(outermost.getQualifiedName().toString());
        }
        for (String key : textAnnos.declAnnos.keySet()) {
            String name = key.indexOf('(') >= 0 ? key.substring(0, key.indexOf('(')) : key;
            while (true) {
                BinaryStubData.ClassRecord rec = data.classes.get(name);
                if (rec != null) {
                    result.add(
                            rec.outerNameIndex == 0 ? name : data.stringPool[rec.outerNameIndex]);
                    break;
                }
                int lastDot = name.lastIndexOf('.');
                if (lastDot < 0) {
                    break;
                }
                name = name.substring(0, lastDot);
            }
        }
        return result;
    }

    /**
     * Verifies that everything the binary record promises was actually stored by {@link
     * BinaryStubReader#applyClassRecord}: an annotated type-parameter record must have produced an
     * {@code atypes} entry for its {@code TypeParameterElement}, a method record carrying type
     * information must have produced an {@code atypes} entry for its {@code ExecutableElement}, and
     * a method record that matches no declared method but an inherited one — a fake override,
     * whether or not it carries any annotations — must have produced a {@code fakeOverrides} entry.
     * This writer-to-reader presence check guards positions (such as type-variable declaration
     * bounds) that {@link #compareAtm} cannot compare against the text parser because the two paths
     * build them from different baselines; a reader change that silently skips applying a record —
     * like the historical {@code hasAnyTypeAnnos} gap that dropped {@code StackWalker.walk}'s
     * {@code <T extends @Nullable Object>} bound — fails here.
     *
     * @param className the fully-qualified name of the class
     * @param cr the binary class record
     * @param data the binary stub data that {@code cr} belongs to, providing its string pool (a
     *     record's indices are only meaningful against its own file's pools)
     * @param binaryAnnos the annotations produced from the record by the binary reader
     * @param elementTypes the stub-types AFET, used for signature lookups
     * @param atypeFactory the factory used to resolve elements
     * @param reports the list to append failure descriptions to
     */
    private static void verifyRecordApplied(
            String className,
            BinaryStubData.ClassRecord cr,
            BinaryStubData data,
            AnnotationFileAnnotations binaryAnnos,
            AnnotationFileElementTypes elementTypes,
            AnnotatedTypeFactory atypeFactory,
            List<String> reports) {
        @SuppressWarnings("signature:argument.type.incompatible") // className is read from the
        // binary stub's string pool, which the binary stub writer populates only with
        // fully-qualified names.
        TypeElement typeElt =
                atypeFactory.getProcessingEnv().getElementUtils().getTypeElement(className);
        if (typeElt == null) {
            return;
        }
        if (BinaryStubReader.classRecordKind(typeElt.getKind()) != cr.kind) {
            // applyClassRecord skipped this record entirely for the same reason (see its own
            // comment): nothing was applied, so there is nothing to verify here either.
            return;
        }
        List<? extends Element> classTypeParams = typeElt.getTypeParameters();
        for (int i = 0; i < cr.typeParams.length && i < classTypeParams.size(); i++) {
            if (BinaryStubReader.typeParamRecordHasAnnos(cr.typeParams[i])
                    && !binaryAnnos.atypes.containsKey(classTypeParams.get(i))) {
                reports.add(
                        String.format(
                                "%s: class type-parameter %d has annotations in the binary record"
                                        + " but no atypes entry was stored",
                                className, i));
            }
            verifyTypeParamAnnosApplied(
                    className,
                    "class type-parameter " + i,
                    cr.typeParams[i],
                    binaryAnnos.atypes.get(classTypeParams.get(i)),
                    data,
                    atypeFactory,
                    reports);
        }
        for (BinaryStubData.MethodRecord mr : cr.methods) {
            String sig = data.stringPool[mr.sigIndex];
            boolean isConstructor = sig.startsWith(BinaryStubData.CONSTRUCTOR_SIG_PREFIX);
            ExecutableElement ee =
                    isConstructor
                            ? elementTypes.constructorSigIndex(typeElt).get(sig)
                            : elementTypes.methodSigIndex(typeElt).get(sig);
            if (ee != null) {
                if (BinaryStubReader.hasTypeInfo(mr) && !binaryAnnos.atypes.containsKey(ee)) {
                    reports.add(
                            String.format(
                                    "%s: method %s has type information in the binary record but"
                                            + " no atypes entry was stored",
                                    className, sig));
                }
                // Per type parameter, not just per method: the method's atypes entry exists
                // whenever the record carries any type information at all, so its presence says
                // nothing about whether a given type parameter's bounds were applied. This is what
                // covers the shape written <T extends @Nullable Object>, as on StackWalker.walk.
                List<? extends Element> methodTypeParams = ee.getTypeParameters();
                for (int i = 0; i < mr.typeParams.length && i < methodTypeParams.size(); i++) {
                    verifyTypeParamAnnosApplied(
                            className,
                            "method " + sig + " type-parameter " + i,
                            mr.typeParams[i],
                            binaryAnnos.atypes.get(methodTypeParams.get(i)),
                            data,
                            atypeFactory,
                            reports);
                }
            } else if (!isConstructor) {
                // The record is a fake override (or a stub/JDK mismatch, in which case there is
                // no overridden method and nothing to store). Checked for EVERY record, even one
                // carrying no annotations at all: both paths store a fakeOverrides entry for
                // every matched fake-override record, because the entry's presence alone resets
                // the method's type at this subtype (see BinaryStubReader#applyFakeOverride).
                verifyFakeOverrideStored(
                        className, sig, typeElt, binaryAnnos, elementTypes, atypeFactory, reports);
            }
        }
        // Presence-only entries (annotated-JDK writer only): a signature the stub source declares
        // with no annotations. If the real class declares the method, the entry is a no-op and
        // nothing needs to have been stored; otherwise it is a fake override whose presence alone
        // must have produced a fakeOverrides entry, exactly like an all-empty full record.
        for (int sigIdx : cr.presenceOnlyMethodSigs) {
            String sig = data.stringPool[sigIdx];
            if (elementTypes.methodSigIndex(typeElt).get(sig) == null) {
                verifyFakeOverrideStored(
                        className, sig, typeElt, binaryAnnos, elementTypes, atypeFactory, reports);
            }
        }
    }

    /**
     * Verifies that every annotation a type-parameter record names was applied to the type variable
     * the binary reader stored for it, and appends a report for each one that was not. This is what
     * covers a type variable's bounds -- {@code <T extends @Nullable Object>} -- which the
     * structural walk cannot compare (see this class's javadoc).
     *
     * <p>This is an oracle against the record rather than a text-vs-binary comparison, because the
     * two loaders' bounds have different baselines: the text parser bakes defaulting into the bound
     * it stores (the upper bound of {@code java.util.List}'s {@code E} comes out
     * {@code @Initialized @NonNull @UnknownKeyFor}), while the binary reader stores only what the
     * stub wrote and lets the framework default the rest later. Comparing the two reports those
     * defaults as mismatches; asking whether everything the record promises is present does not,
     * since defaulting only ever adds annotations.
     *
     * <p>Only the qualifiers this factory supports are demanded. {@code
     * AnnotatedTypeMirror.addAnnotation} drops an annotation outside the factory's own hierarchy,
     * so a {@code @Nullable} in the record is correctly not applied by, say, the Initialization
     * Checker's factory -- and the differential check runs once per factory of the compound
     * checker.
     *
     * @param className the fully-qualified name of the class, for report messages
     * @param what identifies the type parameter, for report messages
     * @param tp the type-parameter record
     * @param binaryAtm the type the binary reader stored for this type parameter, or null
     * @param data the complete binary stub data, for resolving annotation names
     * @param atypeFactory the factory, whose supported qualifiers decide what must be applied
     * @param reports the list to append failure descriptions to
     */
    private static void verifyTypeParamAnnosApplied(
            String className,
            String what,
            BinaryStubData.TypeParamRecord tp,
            @Nullable AnnotatedTypeMirror binaryAtm,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            List<String> reports) {
        if (binaryAtm == null || !BinaryStubReader.typeParamRecordHasAnnos(tp)) {
            // A missing entry for a record that promises annotations is reported by the caller.
            return;
        }
        Set<String> promised = new TreeSet<>();
        for (int annoIdx : tp.typeVarAnnos) {
            promised.add(data.stringPool[data.annotationPool[annoIdx].nameIndex]);
        }
        for (BinaryStubData.TypeAnno[] boundList : tp.boundAnnos) {
            for (BinaryStubData.TypeAnno ta : boundList) {
                promised.add(data.stringPool[data.annotationPool[ta.annoIndex].nameIndex]);
            }
        }
        promised.removeIf(name -> !atypeFactory.isSupportedQualifier(name));
        if (promised.isEmpty()) {
            return;
        }
        Set<String> applied = new TreeSet<>();
        collectAnnotationNames(
                binaryAtm, applied, Collections.newSetFromMap(new IdentityHashMap<>()));
        promised.removeAll(applied);
        for (String missing : promised) {
            reports.add(
                    String.format(
                            "%s: %s: the binary record names %s but it was not applied to %s",
                            className, what, missing, binaryAtm));
        }
    }

    /**
     * Collects the names of every annotation anywhere in {@code atm}, including on the bounds of a
     * type variable or wildcard. Cycle-safe.
     *
     * @param atm the type to walk
     * @param names the set to add annotation names to
     * @param visited the types already walked
     */
    private static void collectAnnotationNames(
            AnnotatedTypeMirror atm, Set<String> names, Set<AnnotatedTypeMirror> visited) {
        if (!visited.add(atm)) {
            return;
        }
        for (AnnotationMirror am : atm.getAnnotations()) {
            names.add(AnnotationUtils.annotationName(am));
        }
        if (atm instanceof AnnotatedTypeVariable) {
            collectAnnotationNames(((AnnotatedTypeVariable) atm).getUpperBound(), names, visited);
            collectAnnotationNames(((AnnotatedTypeVariable) atm).getLowerBound(), names, visited);
        } else if (atm instanceof AnnotatedWildcardType) {
            collectAnnotationNames(((AnnotatedWildcardType) atm).getExtendsBound(), names, visited);
            collectAnnotationNames(((AnnotatedWildcardType) atm).getSuperBound(), names, visited);
        } else if (atm instanceof AnnotatedIntersectionType) {
            for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) atm).getBounds()) {
                collectAnnotationNames(bound, names, visited);
            }
        } else if (atm instanceof AnnotatedDeclaredType) {
            for (AnnotatedTypeMirror targ : ((AnnotatedDeclaredType) atm).getTypeArguments()) {
                collectAnnotationNames(targ, names, visited);
            }
        } else if (atm instanceof AnnotatedArrayType) {
            collectAnnotationNames(((AnnotatedArrayType) atm).getComponentType(), names, visited);
        }
    }

    /**
     * Verifies that, if {@code sig} (which matches no method the real class declares) matches an
     * inherited method, a {@code fakeOverrides} entry keyed by the overridden method with this
     * class as its location was stored. Appends a report otherwise.
     *
     * @param className the fully-qualified name of the class, for report messages
     * @param sig the method's simple signature
     * @param typeElt the class the stub declared the method on
     * @param binaryAnnos the annotations produced by the binary reader
     * @param elementTypes the stub-types AFET, used for signature lookups
     * @param atypeFactory the factory, used for type utilities
     * @param reports the list to append failure descriptions to
     */
    private static void verifyFakeOverrideStored(
            String className,
            String sig,
            TypeElement typeElt,
            AnnotationFileAnnotations binaryAnnos,
            AnnotationFileElementTypes elementTypes,
            AnnotatedTypeFactory atypeFactory,
            List<String> reports) {
        ExecutableElement overridden =
                BinaryStubReader.findFakeOverriddenMethod(typeElt, sig, elementTypes);
        if (overridden == null) {
            return;
        }
        boolean stored = false;
        List<IPair<TypeMirror, AnnotatedTypeMirror>> entries =
                binaryAnnos.fakeOverrides.get(overridden);
        if (entries != null) {
            Types types = atypeFactory.getProcessingEnv().getTypeUtils();
            TypeMirror location = typeElt.asType();
            for (IPair<TypeMirror, AnnotatedTypeMirror> pair : entries) {
                if (types.isSameType(pair.first, location)) {
                    stored = true;
                    break;
                }
            }
        }
        if (!stored) {
            reports.add(
                    String.format(
                            "%s: fake override %s matches an inherited method but no"
                                    + " fakeOverrides entry was stored",
                            className, sig));
        }
    }

    /**
     * Compares the text-parsed and binary-loaded annotations for one top-level class (including its
     * inner classes) and appends a description of each mismatch to {@code reports}.
     *
     * @param className the fully-qualified name of the top-level class, for report messages
     * @param text the annotations produced by the text parser
     * @param binary the annotations produced by the binary reader
     * @param atypeFactory the factory, for type utilities and for recognizing aliased annotations
     * @param reports the list to append mismatch descriptions to
     */
    private static void compareClass(
            String className,
            AnnotationFileAnnotations text,
            AnnotationFileAnnotations binary,
            AnnotatedTypeFactory atypeFactory,
            List<String> reports) {
        Types types = atypeFactory.getProcessingEnv().getTypeUtils();
        // Declaration annotations: same keys, and per key the same annotation names. The
        // comparison is restricted to Checker Framework annotations — the payload the stub
        // pipeline exists to deliver. Whether java.* and JDK-internal declaration annotations
        // (e.g. an unimported @Override, or a @DefinedBy resolvable only through a static
        // import) are kept differs between the two paths due to well-understood text-parser
        // name-resolution quirks, and no checker consumes them from stubs.
        Set<String> declKeys = new TreeSet<>();
        declKeys.addAll(text.declAnnos.keySet());
        declKeys.addAll(binary.declAnnos.keySet());
        for (String key : declKeys) {
            Set<String> textNames = comparedAnnotationNames(text.declAnnos.get(key), atypeFactory);
            Set<String> binaryNames =
                    comparedAnnotationNames(binary.declAnnos.get(key), atypeFactory);
            if (!textNames.equals(binaryNames)) {
                reports.add(
                        String.format(
                                "%s: declAnnos[%s]: text=%s binary=%s",
                                className, key, textNames, binaryNames));
            }
        }

        // Annotated types: per common element key a structural comparison; one-sided entries are
        // a mismatch only if they carry annotations.
        Set<Element> atypeKeys = new HashSet<>();
        atypeKeys.addAll(text.atypes.keySet());
        atypeKeys.addAll(binary.atypes.keySet());
        for (Element key : atypeKeys) {
            AnnotatedTypeMirror textAtm = text.atypes.get(key);
            AnnotatedTypeMirror binaryAtm = binary.atypes.get(key);
            if (textAtm == null) {
                if (hasAnyAnnotation(binaryAtm)) {
                    reports.add(
                            String.format(
                                    "%s: atypes[%s]: only in binary: %s",
                                    className, key, binaryAtm));
                }
                continue;
            }
            if (binaryAtm == null) {
                if (hasNonDerivedAnnotation(
                        textAtm, false, Collections.newSetFromMap(new IdentityHashMap<>()))) {
                    reports.add(
                            String.format(
                                    "%s: atypes[%s]: only in text: %s", className, key, textAtm));
                }
                continue;
            }
            List<String> componentDiffs = new ArrayList<>();
            compareAtm(textAtm, binaryAtm, "", componentDiffs, new IdentityHashMap<>());
            for (String diff : componentDiffs) {
                reports.add(String.format("%s: atypes[%s]: %s", className, key, diff));
            }
        }

        // Record stubs: both sides should agree on which classes are records and which components
        // they have. Annotation agreement is already guaranteed by the atypes/declAnnos checks
        // above (the binary reader stores component types in atypes[componentElement] and
        // declaration annotations in declAnnos[componentElement]). We only verify structural
        // agreement: same record keys and same component names in the same order.
        Set<String> textRecordKeys = text.records.keySet();
        Set<String> binaryRecordKeys = binary.records.keySet();
        for (Map.Entry<String, AnnotationFileParser.RecordStub> textEntry :
                text.records.entrySet()) {
            String key = textEntry.getKey();
            AnnotationFileParser.RecordStub binaryRecord = binary.records.get(key);
            if (binaryRecord == null) {
                reports.add(className + ": records[" + key + "]: in text but not in binary");
            } else {
                List<String> textComponents =
                        new ArrayList<>(textEntry.getValue().componentsByName.keySet());
                List<String> binaryComponents =
                        new ArrayList<>(binaryRecord.componentsByName.keySet());
                if (!textComponents.equals(binaryComponents)) {
                    reports.add(
                            String.format(
                                    "%s: records[%s]: component names differ: text=%s binary=%s",
                                    className, key, textComponents, binaryComponents));
                }
            }
        }
        for (String key : binaryRecordKeys) {
            if (!textRecordKeys.contains(key)) {
                reports.add(className + ": records[" + key + "]: in binary but not in text");
            }
        }
        // Fake overrides: every binary-side entry must have a text-side counterpart with the
        // same fake location. Both paths now store a fakeOverrides entry for every matched
        // fake-override record, annotated or not (BinaryStubReader#applyFakeOverride no longer
        // special-cases an empty returnTypeAnnos, matching
        // AnnotationFileParser#processFakeOverride, which never did): a fake override resets
        // the method's type at that subtype regardless of whether the declaration itself carries
        // any annotations, so an unannotated one is not a no-op to skip. A dropped fake override
        // (annotated or not) is caught by the record oracle in verifyRecordApplied. The method
        // types themselves are not compared here: both sides bake time-dependent defaults via
        // getAnnotatedType, which differ between the text parse and the binary application even
        // when the stub annotations agree.
        for (Map.Entry<ExecutableElement, List<IPair<TypeMirror, AnnotatedTypeMirror>>> entry :
                binary.fakeOverrides.entrySet()) {
            List<IPair<TypeMirror, AnnotatedTypeMirror>> textList =
                    text.fakeOverrides.get(entry.getKey());
            for (IPair<TypeMirror, AnnotatedTypeMirror> binaryPair : entry.getValue()) {
                boolean locationMatched = false;
                if (textList != null) {
                    for (IPair<TypeMirror, AnnotatedTypeMirror> textPair : textList) {
                        if (types.isSameType(textPair.first, binaryPair.first)) {
                            locationMatched = true;
                            break;
                        }
                    }
                }
                if (!locationMatched) {
                    reports.add(
                            String.format(
                                    "%s: fakeOverrides[%s]: binary location %s has no text"
                                            + " counterpart",
                                    className, entry.getKey(), binaryPair.first));
                }
            }
        }
        // And the reverse direction. The binary path resolves a fake override by looking its
        // signature up in a signature index (BinaryStubReader#findFakeOverriddenMethod), while the
        // text path matches parameters structurally (AnnotationFileParser#fakeOverriddenMethod),
        // so the binary path can fail to resolve one that the text path applies. That divergence
        // is invisible to the loop above, and to verifyRecordApplied, whose fake-override check is
        // itself gated on the binary lookup succeeding.
        for (Map.Entry<ExecutableElement, List<IPair<TypeMirror, AnnotatedTypeMirror>>> entry :
                text.fakeOverrides.entrySet()) {
            List<IPair<TypeMirror, AnnotatedTypeMirror>> binaryList =
                    binary.fakeOverrides.get(entry.getKey());
            for (IPair<TypeMirror, AnnotatedTypeMirror> textPair : entry.getValue()) {
                boolean locationMatched = false;
                if (binaryList != null) {
                    for (IPair<TypeMirror, AnnotatedTypeMirror> binaryPair : binaryList) {
                        if (types.isSameType(binaryPair.first, textPair.first)) {
                            locationMatched = true;
                            break;
                        }
                    }
                }
                if (!locationMatched) {
                    reports.add(
                            String.format(
                                    "%s: fakeOverrides[%s]: text location %s has no binary"
                                            + " counterpart",
                                    className, entry.getKey(), textPair.first));
                }
            }
        }
    }

    /**
     * Compares the annotations of two annotated types component by component, appending a
     * description of each differing component to {@code diffs}. The walk is cycle-safe: an
     * (already-visited text component, binary component) pair is not revisited, which terminates
     * recursion through F-bounded type variables such as {@code Enum<E extends Enum<E>>}.
     *
     * <p>Derived positions are excluded from the comparison: the bounds of implicit (unbounded)
     * wildcards and the bounds of type-variable <em>uses</em> cannot carry source annotations —
     * whatever annotations they hold were computed at parse/load time from the type-parameter
     * declaration (the text parser bakes parse-time {@code getAnnotatedType} results into them,
     * which is order-dependent), and {@code mergeAnnotationFileAnnosIntoType}'s target recomputes
     * that information at lookup time. Type-variable <em>declarations</em> ({@code
     * atypes[TypeParameterElement]} entries, the direct type arguments of a class entry, and a
     * method's {@code getTypeVariables()}) are source-annotatable, but their baselines also diverge
     * (fromElement vs. createType), so their explicit annotations are guarded by the record-driven
     * presence check in {@link #verifyRecordApplied} rather than by this comparison.
     *
     * @param text the text parser's type
     * @param binary the binary reader's type
     * @param path human-readable description of the current component's position, e.g. {@code
     *     "return.typeArg[0]"}
     * @param diffs the list to append difference descriptions to
     * @param visited the pairs already visited, to terminate cycles
     */
    private static void compareAtm(
            AnnotatedTypeMirror text,
            AnnotatedTypeMirror binary,
            String path,
            List<String> diffs,
            IdentityHashMap<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visited) {
        if (text == null || binary == null) {
            if ((text == null) != (binary == null)) {
                diffs.add(path + ": one side has no component: text=" + text + " binary=" + binary);
            }
            return;
        }
        Set<AnnotatedTypeMirror> visitedBinaries =
                visited.computeIfAbsent(
                        text, k -> Collections.newSetFromMap(new IdentityHashMap<>()));
        if (!visitedBinaries.add(binary)) {
            return;
        }

        Set<String> textNames = annotationNames(text.getAnnotations());
        Set<String> binaryNames = annotationNames(binary.getAnnotations());
        if (!textNames.equals(binaryNames)) {
            diffs.add(
                    String.format(
                            "%s: text=%s binary=%s",
                            path.isEmpty() ? "top" : path, textNames, binaryNames));
        }

        if (text instanceof AnnotatedExecutableType && binary instanceof AnnotatedExecutableType) {
            AnnotatedExecutableType t = (AnnotatedExecutableType) text;
            AnnotatedExecutableType b = (AnnotatedExecutableType) binary;
            compareAtm(t.getReturnType(), b.getReturnType(), path + "return", diffs, visited);
            compareLists(
                    t.getParameterTypes(), b.getParameterTypes(), path + "param", diffs, visited);
            AnnotatedDeclaredType textReceiver = t.getReceiverType();
            AnnotatedDeclaredType binaryReceiver = b.getReceiverType();
            if (textReceiver != null && binaryReceiver != null) {
                compareAtm(textReceiver, binaryReceiver, path + "receiver", diffs, visited);
            }
            compareLists(
                    t.getTypeVariables(), b.getTypeVariables(), path + "typeVar", diffs, visited);
        } else if (text instanceof AnnotatedDeclaredType
                && binary instanceof AnnotatedDeclaredType) {
            compareLists(
                    ((AnnotatedDeclaredType) text).getTypeArguments(),
                    ((AnnotatedDeclaredType) binary).getTypeArguments(),
                    path + ".typeArg",
                    diffs,
                    visited);
        } else if (text instanceof AnnotatedArrayType && binary instanceof AnnotatedArrayType) {
            compareAtm(
                    ((AnnotatedArrayType) text).getComponentType(),
                    ((AnnotatedArrayType) binary).getComponentType(),
                    path + ".component",
                    diffs,
                    visited);
        } else if (text instanceof AnnotatedWildcardType
                && binary instanceof AnnotatedWildcardType) {
            // Only explicit (source-written) wildcard bounds are compared; the bounds of an
            // unbounded wildcard are derived state (see the method comment).
            WildcardType underlying = ((AnnotatedWildcardType) text).getUnderlyingType();
            if (underlying.getExtendsBound() != null) {
                compareAtm(
                        ((AnnotatedWildcardType) text).getExtendsBound(),
                        ((AnnotatedWildcardType) binary).getExtendsBound(),
                        path + ".extendsBound",
                        diffs,
                        visited);
            } else if (underlying.getSuperBound() != null) {
                compareAtm(
                        ((AnnotatedWildcardType) text).getSuperBound(),
                        ((AnnotatedWildcardType) binary).getSuperBound(),
                        path + ".superBound",
                        diffs,
                        visited);
            }
        } else if (text instanceof AnnotatedTypeVariable
                && binary instanceof AnnotatedTypeVariable) {
            // Type-variable bounds are derived state at uses, and baseline-divergent at
            // declarations; see the method comment. Only the primary annotations (compared
            // above) are compared here.
        } else if (text instanceof AnnotatedIntersectionType
                && binary instanceof AnnotatedIntersectionType) {
            compareLists(
                    ((AnnotatedIntersectionType) text).getBounds(),
                    ((AnnotatedIntersectionType) binary).getBounds(),
                    path + ".bound",
                    diffs,
                    visited);
        } else if (text instanceof AnnotatedUnionType && binary instanceof AnnotatedUnionType) {
            compareLists(
                    ((AnnotatedUnionType) text).getAlternatives(),
                    ((AnnotatedUnionType) binary).getAlternatives(),
                    path + ".alternative",
                    diffs,
                    visited);
        } else if (text.getClass() != binary.getClass()) {
            diffs.add(
                    String.format(
                            "%s: different type shapes: text=%s binary=%s",
                            path,
                            text.getClass().getSimpleName(),
                            binary.getClass().getSimpleName()));
        }
    }

    /**
     * Compares two lists of type components pairwise via {@link #compareAtm}.
     *
     * @param text the text parser's components
     * @param binary the binary reader's components
     * @param path human-readable position prefix; an index suffix is appended per component
     * @param diffs the list to append difference descriptions to
     * @param visited the pairs already visited, to terminate cycles
     */
    private static void compareLists(
            List<? extends AnnotatedTypeMirror> text,
            List<? extends AnnotatedTypeMirror> binary,
            String path,
            List<String> diffs,
            IdentityHashMap<AnnotatedTypeMirror, Set<AnnotatedTypeMirror>> visited) {
        if (text.size() != binary.size()) {
            diffs.add(
                    String.format(
                            "%s: component count differs: text=%d binary=%d",
                            path, text.size(), binary.size()));
            return;
        }
        for (int i = 0; i < text.size(); i++) {
            compareAtm(text.get(i), binary.get(i), path + "[" + i + "]", diffs, visited);
        }
    }

    /**
     * Returns the sorted set of annotation class names in the given annotations, or the empty set
     * for {@code null}.
     *
     * @param annos the annotations, may be {@code null}
     * @return the sorted annotation class names
     */
    private static Set<String> annotationNames(
            @Nullable Iterable<? extends AnnotationMirror> annos) {
        Set<String> names = new TreeSet<>();
        if (annos != null) {
            for (AnnotationMirror am : annos) {
                names.add(AnnotationUtils.annotationName(am));
            }
        }
        return names;
    }

    /**
     * Returns the sorted set of annotation class names in the given set, or the empty set for
     * {@code null}.
     *
     * @param annos the annotation set, may be {@code null}
     * @return the sorted annotation class names
     */
    private static Set<String> annotationNames(@Nullable AnnotationMirrorSet annos) {
        return annotationNames((Iterable<? extends AnnotationMirror>) annos);
    }

    /**
     * Returns the sorted set of the names of the annotations in the given set that are compared, or
     * the empty set for {@code null}: every annotation except the Java platform's own, and except
     * {@code CFComment}.
     *
     * <p>An annotation a checker could act on is compared, whichever package it is in: a checker's
     * own qualifiers, and the third-party annotations a checker resolves by name as aliases --
     * {@code org.jspecify.annotations.Nullable}, say, which is what the annotated JDK of the
     * JSpecify reference checker is written in.
     *
     * <p>The Java platform's own annotations ({@code java.}, {@code jdk.}, {@code sun.}, {@code
     * com.sun.}) are excluded because the two sides legitimately disagree about them, in ways that
     * are text-parser name-resolution quirks rather than binary-reader bugs (observed comparing the
     * JDK 21 annotated JDK, under {@code NullnessBinaryStubDiffTest}):
     *
     * <ul>
     *   <li>{@code java.lang.Override}/{@code java.lang.Deprecated}/{@code
     *       java.lang.SuppressWarnings}: the binary reader resolves the
     *       {@code @Retention}/{@code @Target} meta-annotations written on these annotation-type
     *       declarations themselves; the text parser does not import or resolve them there and
     *       drops them.
     *   <li>{@code com.sun.tools.javac.file.JavacFileManager.setPathFactory(..)}: a
     *       {@code @DefinedBy} annotation reachable only via a static import; the text parser's
     *       {@code AnnotationFileParser.findVariableElement(NameExpr)} resolution does not cover
     *       this position, so it drops the annotation while the binary writer's build-time
     *       classpath resolution keeps it.
     *   <li>{@code com.sun.tools.javac.api.ClientCodeWrapper.Trusted}, {@code
     *       java.lang.invoke.LambdaForm.Compiled}, {@code Tree.Kind.DEFAULT_CASE_LABEL}:
     *       JDK-internal meta-annotations on JDK-internal declarations that the text parser never
     *       resolves (they are not imported by the stub source), but that the binary writer,
     *       running against the full JDK classpath at build time, resolves and keeps.
     * </ul>
     *
     * <p>A platform annotation that this checker <em>can</em> act on is compared even so: one it
     * supports as a qualifier, or aliases to one ({@code jdk.jfr.Unsigned} for the Signedness
     * Checker, {@code com.sun.istack.internal.Interned} for the Interning Checker). The factory
     * decides that, not the package name, so the exclusion never hides an annotation a checker
     * would have consumed.
     *
     * <p>{@code CFComment} is excluded for a different reason: it is documentation for humans with
     * no effect on checking, and the text parser drops it whenever its value uses string
     * concatenation (which it cannot evaluate), whereas the binary writer evaluates the
     * concatenation and keeps it.
     *
     * @param annos the annotation set, may be {@code null}
     * @param atypeFactory the factory, which decides whether this checker can act on a platform
     *     annotation
     * @return the sorted names of the annotations that are compared
     */
    private static Set<String> comparedAnnotationNames(
            @Nullable AnnotationMirrorSet annos, AnnotatedTypeFactory atypeFactory) {
        Set<String> names = new TreeSet<>();
        if (annos != null) {
            for (AnnotationMirror am : annos) {
                // AnnotationUtils.annotationName always returns an interned String (see its
                // @Interned return type and ElementUtils.getQualifiedName's explicit intern()
                // calls, on both the CheckerFrameworkAnnotationMirror fast path and the general
                // one), so comparing against the literal below with != is a correct, cheap
                // identity check, not a bug -- do not "fix" this to .equals().
                String name = AnnotationUtils.annotationName(am);
                if (name == BinaryStubData.CF_COMMENT) {
                    continue;
                }
                // A platform annotation is excluded only if this checker cannot act on it. Asking
                // the factory, rather than trusting the package name, is what makes the rule "an
                // annotation a checker could act on is compared" true: a checker can alias an
                // annotation out of any package, including the platform's own (the Signedness
                // Checker aliases jdk.jfr.Unsigned, the Interning Checker
                // com.sun.istack.internal.Interned).
                if (isPlatformAnnotationName(name)
                        && !atypeFactory.isSupportedQualifier(am)
                        && atypeFactory.canonicalAnnotation(am) == null) {
                    continue;
                }
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Returns true if {@code name} names an annotation declared by the Java platform itself.
     *
     * @param name the fully-qualified name of an annotation
     * @return true if the annotation is the Java platform's own
     */
    private static boolean isPlatformAnnotationName(String name) {
        return name.startsWith("java.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.");
    }

    /**
     * Returns true if {@code atm} carries an annotation anywhere outside a derived position. A
     * derived position is inside the bounds of a wildcard or of any {@code AnnotatedTypeVariable}
     * -- this method does not distinguish a type-variable declaration (e.g. the {@code T} of {@code
     * <T extends Foo>} itself) from a type-variable use (e.g. a parameter of type {@code T}): both
     * are bounds source code cannot annotate directly, so any annotation there was computed by the
     * text parser (implicit-wildcard-bound and type-variable-bound copying) from information that
     * is available elsewhere. An entry stored only by the text parser whose every annotation is in
     * a derived position is therefore benign: the binary path stores no entry, and checking
     * recomputes the same information from the type-parameter declarations.
     *
     * <p>By contrast, if the member had any source annotation, the binary writer recorded it and
     * the binary reader stores an entry, so the entry would not be one-sided in the first place.
     *
     * @param atm the type to scan, may be {@code null}
     * @param inDerivedPosition true if the current component is inside a wildcard or type-variable
     *     bound
     * @param visited components already visited, to terminate cycles through F-bounded types
     * @return true if an annotation exists outside derived positions
     */
    private static boolean hasNonDerivedAnnotation(
            @Nullable AnnotatedTypeMirror atm,
            boolean inDerivedPosition,
            Set<AnnotatedTypeMirror> visited) {
        if (atm == null || !visited.add(atm)) {
            return false;
        }
        if (!inDerivedPosition && !atm.getAnnotations().isEmpty()) {
            return true;
        }
        if (atm instanceof AnnotatedExecutableType) {
            AnnotatedExecutableType exe = (AnnotatedExecutableType) atm;
            if (hasNonDerivedAnnotation(exe.getReturnType(), inDerivedPosition, visited)
                    || hasNonDerivedAnnotation(exe.getReceiverType(), inDerivedPosition, visited)) {
                return true;
            }
            for (AnnotatedTypeMirror param : exe.getParameterTypes()) {
                if (hasNonDerivedAnnotation(param, inDerivedPosition, visited)) {
                    return true;
                }
            }
            for (AnnotatedTypeMirror typeVar : exe.getTypeVariables()) {
                if (hasNonDerivedAnnotation(typeVar, inDerivedPosition, visited)) {
                    return true;
                }
            }
            return false;
        } else if (atm instanceof AnnotatedDeclaredType) {
            for (AnnotatedTypeMirror typeArg : ((AnnotatedDeclaredType) atm).getTypeArguments()) {
                if (hasNonDerivedAnnotation(typeArg, inDerivedPosition, visited)) {
                    return true;
                }
            }
            return false;
        } else if (atm instanceof AnnotatedArrayType) {
            return hasNonDerivedAnnotation(
                    ((AnnotatedArrayType) atm).getComponentType(), inDerivedPosition, visited);
        } else if (atm instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) atm;
            return hasNonDerivedAnnotation(wildcard.getExtendsBound(), true, visited)
                    || hasNonDerivedAnnotation(wildcard.getSuperBound(), true, visited);
        } else if (atm instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) atm;
            return hasNonDerivedAnnotation(typeVar.getUpperBound(), true, visited)
                    || hasNonDerivedAnnotation(typeVar.getLowerBound(), true, visited);
        } else if (atm instanceof AnnotatedIntersectionType) {
            for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) atm).getBounds()) {
                if (hasNonDerivedAnnotation(bound, inDerivedPosition, visited)) {
                    return true;
                }
            }
            return false;
        } else if (atm instanceof AnnotatedUnionType) {
            for (AnnotatedTypeMirror alt : ((AnnotatedUnionType) atm).getAlternatives()) {
                if (hasNonDerivedAnnotation(alt, inDerivedPosition, visited)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /**
     * Returns true if any component of {@code atm} carries any annotation. Used to decide whether
     * an entry present on only one side is a real mismatch.
     *
     * <p>Hand-rolled (rather than a {@code SimpleAnnotatedTypeScanner}, which has no short-circuit
     * support and would always traverse the whole type) to return as soon as one annotation is
     * found, mirroring {@link #hasNonDerivedAnnotation}'s structure and cycle-safety in the same
     * file. Unlike that method, every position counts here (there is no "derived" exclusion): the
     * other side of the comparison has no entry at all, so any annotation on this side, anywhere,
     * is a real difference.
     *
     * @param atm the type to scan, may be {@code null}
     * @return true if any component carries an annotation
     */
    private static boolean hasAnyAnnotation(@Nullable AnnotatedTypeMirror atm) {
        return hasAnyAnnotation(atm, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * Worker for {@link #hasAnyAnnotation(AnnotatedTypeMirror)}.
     *
     * @param atm the type to scan, may be {@code null}
     * @param visited components already visited, to terminate cycles through F-bounded types
     * @return true if any component carries an annotation
     */
    private static boolean hasAnyAnnotation(
            @Nullable AnnotatedTypeMirror atm, Set<AnnotatedTypeMirror> visited) {
        if (atm == null || !visited.add(atm)) {
            return false;
        }
        if (!atm.getAnnotations().isEmpty()) {
            return true;
        }
        if (atm instanceof AnnotatedExecutableType) {
            AnnotatedExecutableType exe = (AnnotatedExecutableType) atm;
            if (hasAnyAnnotation(exe.getReturnType(), visited)
                    || hasAnyAnnotation(exe.getReceiverType(), visited)) {
                return true;
            }
            for (AnnotatedTypeMirror param : exe.getParameterTypes()) {
                if (hasAnyAnnotation(param, visited)) {
                    return true;
                }
            }
            for (AnnotatedTypeMirror typeVar : exe.getTypeVariables()) {
                if (hasAnyAnnotation(typeVar, visited)) {
                    return true;
                }
            }
            return false;
        } else if (atm instanceof AnnotatedDeclaredType) {
            for (AnnotatedTypeMirror typeArg : ((AnnotatedDeclaredType) atm).getTypeArguments()) {
                if (hasAnyAnnotation(typeArg, visited)) {
                    return true;
                }
            }
            return false;
        } else if (atm instanceof AnnotatedArrayType) {
            return hasAnyAnnotation(((AnnotatedArrayType) atm).getComponentType(), visited);
        } else if (atm instanceof AnnotatedWildcardType) {
            AnnotatedWildcardType wildcard = (AnnotatedWildcardType) atm;
            return hasAnyAnnotation(wildcard.getExtendsBound(), visited)
                    || hasAnyAnnotation(wildcard.getSuperBound(), visited);
        } else if (atm instanceof AnnotatedTypeVariable) {
            AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) atm;
            return hasAnyAnnotation(typeVar.getUpperBound(), visited)
                    || hasAnyAnnotation(typeVar.getLowerBound(), visited);
        } else if (atm instanceof AnnotatedIntersectionType) {
            for (AnnotatedTypeMirror bound : ((AnnotatedIntersectionType) atm).getBounds()) {
                if (hasAnyAnnotation(bound, visited)) {
                    return true;
                }
            }
            return false;
        } else if (atm instanceof AnnotatedUnionType) {
            for (AnnotatedTypeMirror alt : ((AnnotatedUnionType) atm).getAlternatives()) {
                if (hasAnyAnnotation(alt, visited)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
