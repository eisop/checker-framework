package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.stub.AnnotationFileParser.RecordComponentStub;
import org.checkerframework.framework.stub.AnnotationFileParser.RecordStub;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.UserError;
import org.plumelib.util.IPair;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * Applies annotation data from a {@link BinaryStubData.ClassRecord} to a checker's {@link
 * AnnotationFileAnnotations}. This translates the compact structural binary representation back
 * into {@link AnnotationMirror} and {@link AnnotatedTypeMirror} objects that the framework uses
 * during type-checking, matching the semantics of the text-based {@link AnnotationFileParser}. It
 * serves both the annotated JDK ({@code annotated-jdk.bin.gz}, applied lazily per requested class)
 * and the built-in checker stub files ({@code *.astub.bin.gz}, applied eagerly at checker
 * initialization with {@code @FromStubFile} marking).
 *
 * <p>Annotations that cannot be resolved (e.g. JDK-internal annotations like {@code @DefinedBy}
 * that are absent from the annotation-processor classpath) are silently skipped, matching the
 * behavior of the text-based {@link AnnotationFileParser}. Annotations that are not supported
 * qualifiers of the current checker are filtered out by {@link
 * AnnotatedTypeMirror#addAnnotation(AnnotationMirror)} when they are applied to a type.
 *
 * <p>User-supplied stub files take precedence: if an entry already exists in {@link
 * AnnotationFileAnnotations#atypes} it is not overwritten, and declaration annotations are merged
 * by annotation name rather than replaced.
 *
 * <p>A stub method that the enclosing class only inherits is applied as a fake override (see {@link
 * #applyFakeOverride}). Classes that are not present in the binary stub -- e.g. a file the writer
 * failed to serialize, or a user-supplied {@code -Astubs} file, which is always text-parsed -- fall
 * through to the text-based {@link AnnotationFileParser} path.
 *
 * <p>Semantic equivalence of this class and the text parser is enforced by {@code
 * BinaryStubDiffChecker} (option {@code -AbinaryStubDiffCheck}); run {@code
 * NullnessBinaryStubDiffTest} after changing either side.
 *
 * @see BinaryStubData
 * @see org.checkerframework.framework.stubifier.BinaryStubWriter
 */
public class BinaryStubReader {

    /** Do not instantiate; all methods are static. */
    private BinaryStubReader() {}

    /**
     * Prefix of the simple signature of a constructor, as {@code ElementUtils.getSimpleSignature}
     * writes it: {@code <init>(...)}.
     */
    private static final String CONSTRUCTOR_SIG_PREFIX = "<init>(";

    /**
     * Applies the package and module annotations from the binary stub data. Called eagerly when the
     * binary stub file is loaded, because package and module annotations are global rather than
     * per-class.
     *
     * <p>Declaration annotations are merged into {@code target.declAnnos} by annotation name; see
     * {@link #mergeDeclAnnos} for how {@code fromLazyJdk} controls which source wins a conflict.
     *
     * @param data the complete binary stub data, providing the string pool and annotation pool
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param target the annotation container to store the results in
     * @param fromLazyJdk true if {@code data} is the lazily-loaded annotated JDK (called from
     *     {@code prepJdkStubs}); false if it is an eagerly-applied built-in stub file (called from
     *     {@code applyBinaryStubData})
     */
    public static void applyPackageAndModuleRecords(
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            AnnotationFileAnnotations target,
            boolean fromLazyJdk) {
        for (Map.Entry<String, int[]> entry : data.packages.entrySet()) {
            applyPackageOrModuleRecord(
                    entry.getKey(),
                    entry.getValue(),
                    ElementKind.PACKAGE,
                    data,
                    atypeFactory,
                    elementTypes,
                    target,
                    fromLazyJdk);
        }
        for (Map.Entry<String, int[]> entry : data.modules.entrySet()) {
            applyPackageOrModuleRecord(
                    entry.getKey(),
                    entry.getValue(),
                    ElementKind.MODULE,
                    data,
                    atypeFactory,
                    elementTypes,
                    target,
                    fromLazyJdk);
        }
    }

    /**
     * Applies the declaration annotations of a single package or module record, honoring
     * {@code @AnnotatedFor} the same way {@link #applyClassRecord} does for classes.
     *
     * @param name the fully-qualified package or module name
     * @param annoPoolIndices indices into {@link BinaryStubData#annotationPool} for this record's
     *     declaration annotations
     * @param kind {@link ElementKind#PACKAGE} or {@link ElementKind#MODULE}
     * @param data the complete binary stub data, providing the string pool and annotation pool
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param target the annotation container to store the results in
     * @param fromLazyJdk true if this is the lazily-loaded annotated JDK; false if it is a built-in
     *     stub file
     */
    private static void applyPackageOrModuleRecord(
            String name,
            int[] annoPoolIndices,
            ElementKind kind,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            AnnotationFileAnnotations target,
            boolean fromLazyJdk) {
        AnnotationMirrorSet declAnnos =
                parseDeclAnnos(annoPoolIndices, null, data, atypeFactory, elementTypes);
        // @AnnotatedFor check: match AnnotationFileParser.isAnnotatedForThisChecker -- but only
        // for a built-in stub file (!fromLazyJdk), for the same reason applyClassRecord's
        // identical gate is scoped to fromStubFileAnno != null: isAnnotatedForThisChecker always
        // returns true, without even inspecting the annotation, when parsing the JDK.
        if (!fromLazyJdk && !isAnnotatedForThisChecker(declAnnos, atypeFactory)) {
            return;
        }
        AnnotationMirrorSet applicable = filterApplicable(declAnnos, kind);
        if (!applicable.isEmpty()) {
            mergeDeclAnnos(target, name, applicable, fromLazyJdk);
        }
    }

    /**
     * Returns whether {@code declAnnos} permits this checker to see the enclosing declaration's
     * stub annotations: true unless one of {@code declAnnos} is {@code @AnnotatedFor} and the
     * current checker is not among the checkers it lists. Matches {@code
     * AnnotationFileParser.isAnnotatedForThisChecker} for the non-JDK case (callers are responsible
     * for skipping this check entirely when parsing the JDK, since {@code
     * isAnnotatedForThisChecker} unconditionally returns true there).
     *
     * @param declAnnos the (unfiltered) declaration annotations parsed for the element
     * @param atypeFactory the factory used to evaluate {@code @AnnotatedFor}
     * @return false if {@code declAnnos} contains an {@code @AnnotatedFor} that excludes this
     *     checker; true otherwise
     */
    private static boolean isAnnotatedForThisChecker(
            AnnotationMirrorSet declAnnos, AnnotatedTypeFactory atypeFactory) {
        for (AnnotationMirror am : declAnnos) {
            if (AnnotationUtils.annotationName(am)
                    == "org.checkerframework.framework.qual.AnnotatedFor") {
                return atypeFactory.doesAnnotatedForApplyToThisChecker(am);
            }
        }
        return true;
    }

    /**
     * Applies the annotations from a {@link BinaryStubData.ClassRecord} to the given class and its
     * members. The annotations are converted to {@link AnnotationMirror}s and stored in {@code
     * target}, which is usually {@code elementTypes.annotationFileAnnos} but may be a scratch
     * container (see {@code BinaryStubDiffChecker}).
     *
     * @param cr the class record from the binary stub data
     * @param className the fully-qualified name of the class
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param data the complete binary stub data, providing the string pool and annotation pool
     * @param target the annotation container to store the results in
     */
    public static void applyClassRecord(
            BinaryStubData.ClassRecord cr,
            String className,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotationFileAnnotations target) {
        applyClassRecord(cr, className, atypeFactory, elementTypes, data, target, null);
    }

    /**
     * Like {@link #applyClassRecord(BinaryStubData.ClassRecord, String, AnnotatedTypeFactory,
     * AnnotationFileElementTypes, BinaryStubData, AnnotationFileAnnotations)}, but additionally
     * marks every matched method and field element with the given {@code @FromStubFile} mirror.
     * Used for built-in checker stub files, whose elements the text parser marks (see {@code
     * AnnotationFileParser.markAsFromStubFile}); the annotated JDK is never marked.
     *
     * @param cr the class record from the binary stub data
     * @param className the fully-qualified name of the class
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param data the complete binary stub data, providing the string pool and annotation pool
     * @param target the annotation container to store the results in
     * @param fromStubFileAnno the {@code @FromStubFile} mirror to mark matched members with, or
     *     {@code null} to not mark (the annotated-JDK case)
     */
    public static void applyClassRecord(
            BinaryStubData.ClassRecord cr,
            String className,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotationFileAnnotations target,
            @Nullable AnnotationMirror fromStubFileAnno) {

        @SuppressWarnings("signature:argument.type.incompatible") // className is read from the
        // binary stub's string pool, which BinaryStubWriter populates only with fully-qualified
        // names
        TypeElement typeElt =
                atypeFactory.getProcessingEnv().getElementUtils().getTypeElement(className);
        if (typeElt == null) {
            // The class does not exist in the JDK that is being compiled against, e.g. when
            // compiling with an older --release version. Nothing to annotate.
            return;
        }
        // Kind check: match AnnotationFileParser.processTypeDecl's own defensive handling of
        // exactly this drift (e.g. it warns and skips java.nio.ByteOrder, which became a real
        // enum in JDK 26 after being a plain class through JDK 25, when the annotated JDK's own
        // stub source still declares it as a class). The annotated JDK is meant to work across
        // JDK versions whose real API can differ in kind from a fixed stub source; a stub written
        // for one kind of declaration must not be blindly applied to a differently-kinded real
        // element; member-level records (fields, methods) would not line up either.
        if (classRecordKind(typeElt.getKind()) != cr.kind) {
            return;
        }

        // Whether a type is already stored for this class BEFORE any of this class record's own
        // processing runs -- e.g. because a user-supplied stub file (-Astubs) was parsed earlier
        // and already established Optional's type. Captured up front, since applyClassTypeParams
        // below may itself populate target.atypes[typeElt] once this method starts running;
        // testing containsKey() again afterwards would then always be true and could never detect
        // the JDK-vs-user-stub case this guards against. This is why the class's own type cannot
        // go through storeOrMerge, which every other site here uses: storeOrMerge decides against
        // the map as it stands when it is called.
        boolean typeAlreadyPresent = target.atypes.containsKey(typeElt);

        // Apply class declaration annotations, merged by name with any existing entry. Only
        // annotations whose @Target permits use on this kind of declaration are recorded as
        // declaration annotations; TYPE_USE-only annotations are applied to the class's type
        // below instead.
        AnnotationMirrorSet classDeclAnnos =
                parseDeclAnnos(cr.declAnnos, className, data, atypeFactory, elementTypes);
        // @AnnotatedFor check: match AnnotationFileParser.isAnnotatedForThisChecker — but only for
        // a built-in stub file (fromStubFileAnno != null). isAnnotatedForThisChecker always
        // returns true, without even inspecting the annotation, when parsing the JDK
        // (fileType == JDK_STUB, the fromStubFileAnno == null case here): "The JDK stubs have
        // purity annotations that should be read for all checkers." Applying this skip
        // unconditionally regressed hundreds of JDK classes carrying @AnnotatedFor for an
        // unrelated checker (e.g. @AnnotatedFor("interning")) to have no declaration annotations
        // at all under every other checker.
        if (fromStubFileAnno != null && !isAnnotatedForThisChecker(classDeclAnnos, atypeFactory)) {
            return;
        }
        if (!classDeclAnnos.isEmpty()) {
            AnnotationMirrorSet applicable = filterApplicable(classDeclAnnos, typeElt.getKind());
            if (!applicable.isEmpty()) {
                mergeDeclAnnos(
                        target,
                        ElementUtils.getQualifiedName(typeElt),
                        applicable,
                        fromStubFileAnno == null);
            }
        }

        // Apply class type-parameter annotations (e.g. <K extends @NonNull Object>). This also
        // stores the class's AnnotatedDeclaredType keyed by the TypeElement: fresh if
        // typeAlreadyPresent is false, merged onto the existing entry if fromStubFileAnno != null
        // (an earlier built-in stub file), or left untouched otherwise (the lazily-loaded JDK).
        // cr.typeParams.length > 0 only means the class is generic (e.g. "class Foo<T>"), not that
        // any type parameter is actually annotated -- checking typeParamRecordHasAnnos first avoids
        // building an AnnotatedDeclaredType (createType, below) for every generic class in the
        // source tree when none of its type parameters carry an annotation. If this is skipped, the
        // class-level-annotation code below still creates the stored type on demand (see its
        // "cr.typeParams was empty" comment, which also covers this case).
        boolean anyTypeParamAnnotated = false;
        for (BinaryStubData.TypeParamRecord tp : cr.typeParams) {
            if (typeParamRecordHasAnnos(tp)) {
                anyTypeParamAnnotated = true;
                break;
            }
        }
        if (anyTypeParamAnnotated) {
            applyClassTypeParams(
                    cr,
                    className,
                    typeElt,
                    atypeFactory,
                    elementTypes,
                    data,
                    target,
                    fromStubFileAnno);
        }

        // Apply class-level annotations (e.g. @NonNull on "class Optional<T>", which forbids
        // @Nullable Optional -- see the class's own javadoc in the annotated JDK for why, and how
        // a user stub is meant to override it) to the AnnotatedDeclaredType stored in
        // atypes[typeElt], matching what the text parser does in processType via annotate(type,
        // decl.getAnnotations(), decl) followed by putMerge. putMerge's behavior when a type is
        // already stored depends on whether the *new* content is from the lazily-loaded JDK
        // (fromStubFileAnno == null) or an eagerly-applied built-in stub file: the JDK must never
        // replace an existing entry (it is always applied last, on demand, so anything already
        // present -- from a user-supplied -Astubs file or an earlier built-in stub -- takes
        // precedence); a built-in stub file, however, may replace an entry from an *earlier*
        // built-in stub file, matching parseStubFiles's documented "the qualifier in the last stub
        // file is applied" rule (e.g. nullness's junit-assertions.astub and a later @StubFiles
        // entry could both annotate the same inherited method). Annotations that are not supported
        // qualifiers of the current checker are filtered out by AnnotatedTypeMirror.addAnnotation,
        // which replaceAnnotation delegates to.
        if (!classDeclAnnos.isEmpty()) {
            if (!typeAlreadyPresent) {
                AnnotatedTypeMirror storedType = target.atypes.get(typeElt);
                if (storedType == null) {
                    // cr.typeParams was empty, so applyClassTypeParams did not run; create one now.
                    storedType =
                            AnnotatedTypeMirror.createType(typeElt.asType(), atypeFactory, false);
                    target.atypes.put(typeElt, storedType);
                }
                for (AnnotationMirror am : classDeclAnnos) {
                    storedType.replaceAnnotation(am);
                }
            } else if (fromStubFileAnno != null) {
                // An entry already exists (e.g. from an earlier built-in stub file), and this
                // content is itself from a built-in stub file (not the lazily-loaded JDK): merge
                // onto a fresh type, exactly as AnnotationFileParser.putMerge does via
                // atypeFactory.replaceAnnotations(newType, existingType).
                AnnotatedTypeMirror freshType =
                        AnnotatedTypeMirror.createType(typeElt.asType(), atypeFactory, false);
                for (AnnotationMirror am : classDeclAnnos) {
                    freshType.replaceAnnotation(am);
                }
                atypeFactory.replaceAnnotations(freshType, target.atypes.get(typeElt));
            }
            // else: fromStubFileAnno == null (the lazily-loaded JDK) and an entry already exists
            // from an earlier source -- leave it untouched, matching putMerge's JDK_STUB case.
        }

        if (cr.fields.length > 0) {
            applyFieldRecords(
                    cr,
                    className,
                    typeElt,
                    atypeFactory,
                    elementTypes,
                    data,
                    target,
                    fromStubFileAnno);
        }

        if (cr.methods.length > 0) {
            applyMethodRecords(
                    cr,
                    className,
                    typeElt,
                    atypeFactory,
                    elementTypes,
                    data,
                    target,
                    fromStubFileAnno);
        }

        if (cr.kind == BinaryStubData.ClassRecord.KIND_RECORD) {
            applyRecordComponents(cr, className, typeElt, atypeFactory, elementTypes, data, target);
        }
    }

    /**
     * Returns the {@code BinaryStubData.ClassRecord.KIND_*} constant corresponding to {@code kind},
     * for the {@code ENUM}, {@code ANNOTATION_TYPE}, {@code CLASS}, {@code INTERFACE}, and {@code
     * RECORD} kinds {@code BinaryStubWriter} writes a class record for, or {@code -1} for any other
     * {@code ElementKind}. The {@code -1} sentinel can never equal a real {@code ClassRecord.kind}
     * value, so it forces a mismatch and causes the record to be skipped. {@code
     * ElementKind.RECORD} itself is deliberately not referenced here by name, since it does not
     * exist before JDK 16 and this class must compile against older JDKs too.
     *
     * @param kind the real {@code TypeElement}'s kind
     * @return the corresponding {@code KIND_*} constant, or {@code -1} if none corresponds
     */
    static int classRecordKind(ElementKind kind) {
        switch (kind) {
            case ENUM:
                return BinaryStubData.ClassRecord.KIND_ENUM;
            case ANNOTATION_TYPE:
                return BinaryStubData.ClassRecord.KIND_ANNOTATION_TYPE;
            case CLASS:
            case INTERFACE:
                return BinaryStubData.ClassRecord.KIND_CLASS_OR_INTERFACE;
            default:
                // ElementKind.RECORD does not exist before JDK 16 so it cannot be named here;
                // test by name to stay compatible with older compiler versions.
                if (kind.name().equals("RECORD")) {
                    return BinaryStubData.ClassRecord.KIND_RECORD;
                }
                return -1;
        }
    }

    /**
     * Applies the component records of a record class to build a {@link
     * AnnotationFileParser.RecordStub} and store it in {@code target.records}. This mirrors what
     * {@link AnnotationFileParser} does inside {@code processTypeDecl} for a {@code
     * RecordDeclaration}: it creates a {@code RecordComponentStub} for each component carrying the
     * component's annotated type, declaration annotations, and the {@code hasAccessorInStubs} flag,
     * then stores a {@code RecordStub} keyed by the record's fully-qualified name.
     *
     * @param cr the class record with {@code kind == KIND_RECORD}
     * @param className the fully-qualified name of the record class
     * @param typeElt the element of the record class
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param data the complete binary stub data
     * @param target the annotation container to store the results in
     */
    private static void applyRecordComponents(
            BinaryStubData.ClassRecord cr,
            String className,
            TypeElement typeElt,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotationFileAnnotations target) {
        // Build a name-to-element map for the record's components using the same FIELD elements
        // AnnotationFileParser.findFieldElement uses (ElementFilter.fieldsIn), not
        // TypeElement.getRecordComponents(): a component's RECORD_COMPONENT element is a distinct
        // Element from its compiler-generated backing FIELD (different identity, confirmed: they
        // are not .equals()), and the text parser stores the component's atypes entry keyed by the
        // FIELD. Keying by RECORD_COMPONENT instead would silently make the field's own type-use
        // annotations unreachable through AnnotationFileElementTypes.getAnnotatedTypeMirror, which
        // does a bare Element-keyed lookup with no name-based fallback (unlike getDeclAnnotations,
        // which does have one, for declaration annotations only).
        Map<String, VariableElement> componentByName = new HashMap<>();
        for (VariableElement field : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
            componentByName.put(field.getSimpleName().toString(), field);
        }

        Map<String, RecordComponentStub> byName = new LinkedHashMap<>();
        for (BinaryStubData.ComponentRecord comp : cr.components) {
            String name = data.stringPool[comp.nameIndex];
            VariableElement compElt = componentByName.get(name);
            if (compElt == null) {
                // Component not found in the real element; skip (version skew, same as fields).
                continue;
            }

            // Build the annotated type for this component.
            AnnotatedTypeMirror compType =
                    AnnotatedTypeMirror.createType(compElt.asType(), atypeFactory, false);
            if (comp.typeAnnos.length > 0) {
                applyTypeAnnos(
                        compType, comp.typeAnnos, className, data, atypeFactory, elementTypes);
            }
            // Store keyed by the field element in atypes, matching
            // AnnotationFileParser.processRecordField's putMerge(atypes, elt, fieldType).
            target.atypes.putIfAbsent(compElt, compType);

            // Parse declaration annotations. Merged with fromLazyJdk semantics -- an existing
            // annotation of the same type wins -- because applyRecordComponents has no
            // fromStubFileAnno parameter to distinguish the two sources; applyClassRecord does not
            // pass it one. That is what the code inlined here has always done.
            AnnotationMirrorSet declAnnos =
                    parseDeclAnnos(comp.declAnnos, className, data, atypeFactory, elementTypes);
            if (!declAnnos.isEmpty()) {
                mergeDeclAnnos(
                        target,
                        ElementUtils.getQualifiedName(compElt),
                        declAnnos,
                        /* fromLazyJdk= */ true);
            }

            RecordComponentStub stub = new RecordComponentStub(compType, declAnnos);
            if (comp.hasAccessor) {
                stub.setHasAccessorInStubs();
            }
            byName.put(name, stub);
        }

        RecordStub recordStub = new RecordStub(byName);
        if (cr.canonicalConstructorParamAnnos != null) {
            // An explicit (non-compact) canonical constructor's own parameter annotations override
            // the record components' -- matching AnnotationFileParser's
            // RecordStub#componentsInCanonicalConstructor. Find the real canonical constructor
            // (there is always exactly one per record) and apply the recorded per-parameter
            // annotations to its own parameter types.
            for (ExecutableElement ctor :
                    ElementFilter.constructorsIn(typeElt.getEnclosedElements())) {
                if (!AnnotationFileUtil.isCanonicalConstructor(
                        ctor, atypeFactory.getProcessingEnv().getTypeUtils())) {
                    continue;
                }
                List<? extends VariableElement> ctorParams = ctor.getParameters();
                if (ctorParams.size() != cr.canonicalConstructorParamAnnos.length) {
                    // Version skew: the real canonical constructor's parameter count no longer
                    // matches what the writer recorded. Leave componentsInCanonicalConstructor
                    // unset; RecordStub falls back to the (still valid) per-component annotations.
                    break;
                }
                List<AnnotatedTypeMirror> annotatedParameters = new ArrayList<>(ctorParams.size());
                for (int i = 0; i < ctorParams.size(); i++) {
                    AnnotatedTypeMirror atm =
                            AnnotatedTypeMirror.createType(
                                    ctorParams.get(i).asType(), atypeFactory, false);
                    BinaryStubData.TypeAnno[] paramAnnos = cr.canonicalConstructorParamAnnos[i];
                    if (paramAnnos.length > 0) {
                        applyTypeAnnos(
                                atm, paramAnnos, className, data, atypeFactory, elementTypes);
                    }
                    annotatedParameters.add(atm);
                }
                recordStub.componentsInCanonicalConstructor = annotatedParameters;
                break;
            }
        }

        // Always store a RecordStub for a KIND_RECORD class, even one with no (or no resolvable)
        // components, matching AnnotationFileParser's unconditional
        // annotationFileAnnos.records.put(...) for every RecordDeclaration.
        target.records.put(className, recordStub);
    }

    /**
     * Applies the field records of {@code cr} to the fields of {@code typeElt}.
     *
     * @param cr the class record whose field records to apply
     * @param className the fully-qualified name of the class
     * @param typeElt the element of the class
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param data the complete binary stub data
     * @param target the annotation container to store the results in
     * @param fromStubFileAnno the {@code @FromStubFile} mirror to mark matched fields with, or
     *     {@code null} to not mark
     */
    private static void applyFieldRecords(
            BinaryStubData.ClassRecord cr,
            String className,
            TypeElement typeElt,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotationFileAnnotations target,
            @Nullable AnnotationMirror fromStubFileAnno) {
        Map<String, VariableElement> fieldsByName = new HashMap<>();
        for (VariableElement ve : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
            fieldsByName.put(ve.getSimpleName().toString(), ve);
        }

        for (BinaryStubData.FieldRecord fr : cr.fields) {
            String fieldName = data.stringPool[fr.nameIndex];
            VariableElement ve = fieldsByName.get(fieldName);
            if (ve == null) {
                // The field does not exist in the JDK being compiled against.
                continue;
            }
            if (fromStubFileAnno != null) {
                markFromStubFile(target, ve, fromStubFileAnno);
            }
            AnnotationMirrorSet fieldDeclAnnos =
                    parseApplicableDeclAnnos(
                            fr.declAnnos,
                            ve.getKind(),
                            className,
                            data,
                            atypeFactory,
                            elementTypes);
            if (!fieldDeclAnnos.isEmpty()) {
                mergeDeclAnnos(
                        target,
                        ElementUtils.getQualifiedName(ve),
                        fieldDeclAnnos,
                        fromStubFileAnno == null);
            }
            if (fr.typeAnnos.length > 0) {
                // Skip building the type at all when storeOrMerge would discard it: an entry
                // exists and this content is from the lazily-loaded JDK.
                if (!target.atypes.containsKey(ve) || fromStubFileAnno != null) {
                    AnnotatedTypeMirror atm =
                            AnnotatedTypeMirror.createType(ve.asType(), atypeFactory, false);
                    applyTypeAnnos(atm, fr.typeAnnos, className, data, atypeFactory, elementTypes);
                    storeOrMerge(target, ve, atm, atypeFactory, fromStubFileAnno);
                }
            }
        }
    }

    /**
     * Applies the method and constructor records of {@code cr} to the members of {@code typeElt}.
     *
     * @param cr the class record whose method records to apply
     * @param className the fully-qualified name of the class
     * @param typeElt the element of the class
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param data the complete binary stub data
     * @param target the annotation container to store the results in
     * @param fromStubFileAnno the {@code @FromStubFile} mirror to mark matched methods with, or
     *     {@code null} to not mark
     */
    private static void applyMethodRecords(
            BinaryStubData.ClassRecord cr,
            String className,
            TypeElement typeElt,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotationFileAnnotations target,
            @Nullable AnnotationMirror fromStubFileAnno) {
        for (BinaryStubData.MethodRecord mr : cr.methods) {
            String sig = data.stringPool[mr.sigIndex];
            // Constructor signatures start with "<init>("; use the matching index so the other
            // index is not computed unnecessarily.
            boolean isConstructor = sig.startsWith(CONSTRUCTOR_SIG_PREFIX);
            ExecutableElement ee =
                    isConstructor
                            ? elementTypes.constructorSigIndex(typeElt).get(sig)
                            : elementTypes.methodSigIndex(typeElt).get(sig);
            if (ee == null) {
                // Not declared in this class. The stub may declare a "fake override":
                // annotations on an inherited method that apply only at this subtype (see
                // AnnotationFileParser.processFakeOverride). Otherwise the method does not
                // exist in the JDK being compiled against and there is nothing to apply.
                if (!isConstructor) {
                    applyFakeOverride(
                            mr, sig, typeElt, className, data, atypeFactory, elementTypes, target);
                }
                continue;
            }
            if (fromStubFileAnno != null) {
                markFromStubFile(target, ee, fromStubFileAnno);
            }

            AnnotationMirrorSet methodDeclAnnos =
                    parseApplicableDeclAnnos(
                            mr.declAnnos,
                            ee.getKind(),
                            className,
                            data,
                            atypeFactory,
                            elementTypes);
            if (!methodDeclAnnos.isEmpty()) {
                mergeDeclAnnos(
                        target,
                        ElementUtils.getQualifiedName(ee),
                        methodDeclAnnos,
                        fromStubFileAnno == null);
            }

            if (!hasTypeInfo(mr)) {
                continue;
            }
            // If an entry already exists and this content is from the lazily-loaded JDK, back
            // off entirely (matches applyClassRecord's class-level treatment: the JDK must never
            // replace an entry from an earlier source). Otherwise -- nothing exists yet, or this
            // is itself a built-in stub file that may replace an earlier built-in stub file's
            // entry (last stub file wins) -- proceed to build a fresh AnnotatedExecutableType;
            // which of those two cases applies is decided below, once aet is built.
            boolean methodAlreadyPresent = target.atypes.containsKey(ee);
            if (methodAlreadyPresent && fromStubFileAnno == null) {
                continue;
            }

            AnnotatedExecutableType aet =
                    (AnnotatedExecutableType)
                            AnnotatedTypeMirror.createType(ee.asType(), atypeFactory, false);
            aet.setElement(ee);

            applyTypeAnnos(
                    aet.getReturnType(),
                    mr.returnTypeAnnos,
                    className,
                    data,
                    atypeFactory,
                    elementTypes);

            // A receiver's declared type is always the enclosing class with its own type
            // arguments, so a TYPE_ARGUMENT-indexed receiver annotation is only meaningful if the
            // enclosing class's real type-parameter count (now) matches what the writer recorded
            // (cr.typeParams.length, one entry per declared type parameter -- see
            // BinaryStubWriter.extractTypeParams). A JDK class's own type-parameter count can
            // drift across JDK versions (e.g. java.util.concurrent.ConcurrentSkipListMap.KeySet
            // has one type parameter under JDK 8's real class but two under later JDKs' stub
            // source), in which case applying the recorded index would target the wrong type
            // argument. Skip entirely on a mismatch, matching AnnotationFileParser.annotate's own
            // "Mismatch in type argument size" guard for exactly this case.
            if (aet.getReceiverType() != null
                    && mr.receiverAnnos.length > 0
                    && cr.typeParams.length == typeElt.getTypeParameters().size()) {
                applyTypeAnnos(
                        aet.getReceiverType(),
                        mr.receiverAnnos,
                        className,
                        data,
                        atypeFactory,
                        elementTypes);
            }

            List<? extends AnnotatedTypeMirror> paramTypes = aet.getParameterTypes();
            for (int i = 0; i < mr.paramAnnos.length && i < paramTypes.size(); i++) {
                AnnotatedTypeMirror pType = paramTypes.get(i);
                VariableElement pElt = ee.getParameters().get(i);

                AnnotationMirrorSet paramDeclAnnos =
                        parseDeclAnnos(
                                mr.paramDeclAnnos[i], className, data, atypeFactory, elementTypes);
                if (!paramDeclAnnos.isEmpty()) {
                    AnnotationMirrorSet applicable =
                            filterApplicable(paramDeclAnnos, pElt.getKind());
                    if (!applicable.isEmpty()) {
                        mergeDeclAnnos(
                                target,
                                ElementUtils.getQualifiedName(pElt),
                                applicable,
                                fromStubFileAnno == null);
                    }
                    if (!target.atypes.containsKey(pElt) || fromStubFileAnno != null) {
                        // Declaration annotations that are also type qualifiers apply to the
                        // innermost component type of an array parameter, matching
                        // annotateInnermostComponentType in the text parser. Unsupported
                        // qualifiers are filtered by replaceAnnotation. Applied to the fresh
                        // pType/pInner unconditionally (whether or not pElt already has a stored
                        // entry): the merge-vs-store decision below propagates this either way,
                        // since pType is a component of the fresh aet being built.
                        AnnotatedTypeMirror pInner = AnnotatedTypes.innerMostType(pType);
                        for (AnnotationMirror am : paramDeclAnnos) {
                            pInner.replaceAnnotation(am);
                        }
                    }
                }

                applyTypeAnnos(
                        pType, mr.paramAnnos[i], className, data, atypeFactory, elementTypes);
                if (!(ee.isVarArgs() && i == paramTypes.size() - 1)) {
                    // The text parser does not store an entry for the vararg parameter itself
                    // (see AnnotationFileParser.processParameters); its annotations are part of
                    // the stored method type.
                    storeOrMerge(target, pElt, pType, atypeFactory, fromStubFileAnno);
                }
            }

            // Apply method type-parameter annotations (e.g. <T extends @X Object>). The type
            // variables are annotated in place in aet.getTypeVariables() and also stored keyed
            // by their TypeParameterElement, matching the text parser.
            applyMethodTypeParams(
                    mr.typeParams,
                    aet,
                    ee,
                    className,
                    data,
                    atypeFactory,
                    elementTypes,
                    target,
                    fromStubFileAnno);
            // Propagate class type-parameter bound annotations to type variables in the method
            // type (e.g. K and V in put(K key, V value) of Hashtable).
            propagateClassTypeParamBounds(aet, target);
            // storeOrMerge's third case -- an entry exists and this is the lazily-loaded JDK --
            // is unreachable here: the loop already backed off above.
            storeOrMerge(target, ee, aet, atypeFactory, fromStubFileAnno);
        }
    }

    /**
     * Applies a fake override: a method that a stub file declares on a class that only inherits it.
     * Matching {@code AnnotationFileParser.processFakeOverride}, only the return-type annotations
     * are applied (to a fresh {@code getAnnotatedType} of the overridden method), and the result is
     * stored in {@link AnnotationFileAnnotations#fakeOverrides} keyed by the overridden method,
     * together with the type of the class the stub declared it on.
     *
     * @param mr the method record whose signature did not match any declared method
     * @param sig the method's simple signature
     * @param typeElt the class the stub declared the method on
     * @param className the fully-qualified name of that class
     * @param data the complete binary stub data
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the signature indexes
     * @param target the annotation container to store the results in
     */
    private static void applyFakeOverride(
            BinaryStubData.MethodRecord mr,
            String sig,
            TypeElement typeElt,
            String className,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            AnnotationFileAnnotations target) {
        if (mr.returnTypeAnnos.length == 0) {
            // The text parser applies only return-type annotations to fake overrides.
            return;
        }
        ExecutableElement overridden = findFakeOverriddenMethod(typeElt, sig, elementTypes);
        if (overridden == null) {
            // Also not inherited: the stub does not match the JDK being compiled against.
            // The text parser reports a "not found" warning here and skips the declaration.
            return;
        }
        AnnotatedTypeMirror methodType = atypeFactory.getAnnotatedType(overridden);
        if (!(methodType instanceof AnnotatedExecutableType)) {
            return;
        }
        AnnotatedExecutableType aet = (AnnotatedExecutableType) methodType;
        applyTypeAnnos(
                aet.getReturnType(),
                mr.returnTypeAnnos,
                className,
                data,
                atypeFactory,
                elementTypes);
        target.fakeOverrides
                .computeIfAbsent(overridden, k -> new ArrayList<>(1))
                .add(IPair.of(typeElt.asType(), aet));
    }

    /**
     * Searches the supertypes of {@code typeElt} (superclasses and interfaces, recursively) for a
     * method with the given simple signature, mirroring {@code
     * AnnotationFileParser.fakeOverriddenMethod}.
     *
     * @param typeElt the class whose supertypes to search; its own declared methods have already
     *     been checked by the caller
     * @param sig the method's simple signature
     * @param elementTypes per-factory state, providing the per-class signature indexes
     * @return the overridden method, or {@code null} if no supertype declares one
     */
    static @Nullable ExecutableElement findFakeOverriddenMethod(
            TypeElement typeElt, String sig, AnnotationFileElementTypes elementTypes) {
        return findFakeOverriddenMethod(
                typeElt, sig, elementTypes, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * Worker for {@link #findFakeOverriddenMethod(TypeElement, String,
     * AnnotationFileElementTypes)}.
     *
     * @param typeElt the class whose supertypes to search
     * @param sig the method's simple signature
     * @param elementTypes per-factory state, providing the per-class signature indexes
     * @param visited interfaces already searched, to avoid re-traversing a shared ancestor
     *     interface reachable through more than one path (diamond inheritance)
     * @return the overridden method, or {@code null} if no supertype declares one
     */
    private static @Nullable ExecutableElement findFakeOverriddenMethod(
            TypeElement typeElt,
            String sig,
            AnnotationFileElementTypes elementTypes,
            Set<TypeElement> visited) {
        TypeElement superClass = ElementUtils.getSuperClass(typeElt);
        if (superClass != null) {
            ExecutableElement found = elementTypes.methodSigIndex(superClass).get(sig);
            if (found == null) {
                found = findFakeOverriddenMethod(superClass, sig, elementTypes, visited);
            }
            if (found != null) {
                return found;
            }
        }
        for (TypeMirror interfaceType : typeElt.getInterfaces()) {
            if (interfaceType.getKind() != TypeKind.DECLARED) {
                continue;
            }
            Element interfaceElt = ((javax.lang.model.type.DeclaredType) interfaceType).asElement();
            if (!(interfaceElt instanceof TypeElement)
                    || !visited.add((TypeElement) interfaceElt)) {
                continue;
            }
            ExecutableElement found =
                    elementTypes.methodSigIndex((TypeElement) interfaceElt).get(sig);
            if (found == null) {
                found =
                        findFakeOverriddenMethod(
                                (TypeElement) interfaceElt, sig, elementTypes, visited);
            }
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Returns true if the method record carries any information that requires building and storing
     * an {@link AnnotatedExecutableType}: type annotations on the return, receiver, or parameter
     * types, declaration annotations on parameters, or annotations on the method's type parameters.
     *
     * @param mr the method record to inspect
     * @return true if an {@code AnnotatedExecutableType} must be built for this record
     */
    static boolean hasTypeInfo(BinaryStubData.MethodRecord mr) {
        if (mr.returnTypeAnnos.length > 0 || mr.receiverAnnos.length > 0) {
            return true;
        }
        for (int i = 0; i < mr.paramAnnos.length; i++) {
            if (mr.paramAnnos[i].length > 0 || mr.paramDeclAnnos[i].length > 0) {
                return true;
            }
        }
        for (BinaryStubData.TypeParamRecord tp : mr.typeParams) {
            if (typeParamRecordHasAnnos(tp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the type-parameter record carries any annotations, either on the type
     * variable itself or on any of its bounds.
     *
     * @param tp the type-parameter record to inspect
     * @return true if the record carries any annotations
     */
    static boolean typeParamRecordHasAnnos(BinaryStubData.TypeParamRecord tp) {
        if (tp.typeVarAnnos.length > 0) {
            return true;
        }
        for (BinaryStubData.TypeAnno[] boundList : tp.boundAnnos) {
            if (boundList.length > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Merges declaration annotations for {@code eltName} into {@code target.declAnnos}, matching
     * the text parser's {@code putOrAddToDeclAnnos}. If no entry exists, {@code newAnnos} is stored
     * directly. Otherwise, the merge direction depends on {@code fromLazyJdk}: for the
     * lazily-loaded annotated JDK ({@code true}), only annotations whose type is not already
     * present (compared by name) are added, so a JDK annotation never replaces one from an earlier
     * source (e.g. a user-supplied {@code -Astubs} file, or an earlier built-in stub file — the JDK
     * is always applied last, on demand). For any other (eager, built-in-stub) source ({@code
     * false}), existing annotations of the same type are first removed, so a later-processed stub
     * file's annotation replaces an earlier one, matching {@code parseStubFiles}'s documented "the
     * qualifier in the last stub file is applied" rule.
     *
     * @param target the annotation container to store the results in
     * @param eltName the fully-qualified name of the annotated element
     * @param newAnnos the declaration annotations to merge in
     * @param fromLazyJdk true if these annotations come from the lazily-loaded annotated JDK, which
     *     must never replace an annotation from an earlier source
     */
    private static void mergeDeclAnnos(
            AnnotationFileAnnotations target,
            String eltName,
            AnnotationMirrorSet newAnnos,
            boolean fromLazyJdk) {
        AnnotationMirrorSet existing = target.declAnnos.get(eltName);
        if (existing == null) {
            target.declAnnos.put(eltName, newAnnos);
        } else if (fromLazyJdk) {
            for (AnnotationMirror am : newAnnos) {
                if (!AnnotationUtils.containsSameByName(existing, am)) {
                    existing.add(am);
                }
            }
        } else {
            existing.removeIf(am -> AnnotationUtils.containsSameByName(newAnnos, am));
            existing.addAll(newAnnos);
        }
    }

    /**
     * Stores {@code fresh} in {@code target.atypes} under {@code elt}, or reconciles it with the
     * type already stored there. The counterpart of {@link #mergeDeclAnnos} for annotated types,
     * and the same three-way decision the text parser's {@code putMerge} makes:
     *
     * <ul>
     *   <li>Nothing stored yet: {@code fresh} becomes the stored type.
     *   <li>Something stored, and {@code fresh} comes from a built-in stub file ({@code
     *       fromStubFileAnno != null}): merge onto the existing entry, so a later-processed stub
     *       file's qualifier replaces an earlier one's, matching {@code parseStubFiles}'s
     *       documented "the qualifier in the last stub file is applied" rule.
     *   <li>Something stored, and {@code fresh} comes from the lazily-loaded annotated JDK ({@code
     *       fromStubFileAnno == null}): leave the entry alone. The JDK is applied last, on demand,
     *       so anything already present -- from a user-supplied {@code -Astubs} file or a built-in
     *       stub -- takes precedence.
     * </ul>
     *
     * <p>{@code applyClassRecord} cannot use this for the class's own type: it must decide against
     * whether an entry existed <em>before</em> the class record began processing, since {@code
     * applyClassTypeParams} may itself have stored one in the meantime.
     *
     * @param target the annotation container to store into
     * @param elt the element to key the type by
     * @param fresh the newly built annotated type
     * @param atypeFactory the factory, used to merge onto an existing entry
     * @param fromStubFileAnno the {@code @FromStubFile} mirror if this content is from a built-in
     *     stub file, or {@code null} for the lazily-loaded JDK; only its nullness is consulted
     */
    private static void storeOrMerge(
            AnnotationFileAnnotations target,
            Element elt,
            AnnotatedTypeMirror fresh,
            AnnotatedTypeFactory atypeFactory,
            @Nullable AnnotationMirror fromStubFileAnno) {
        AnnotatedTypeMirror existing = target.atypes.get(elt);
        if (existing == null) {
            target.atypes.put(elt, fresh);
        } else if (fromStubFileAnno != null) {
            atypeFactory.replaceAnnotations(fresh, existing);
        }
    }

    /**
     * Marks {@code elt} as coming from a stub file by merging the {@code @FromStubFile} mirror into
     * its declaration annotations, matching {@code AnnotationFileParser.markAsFromStubFile}. Only
     * ever called for built-in stub files (the annotated JDK is never marked), so this is always a
     * non-JDK merge.
     *
     * @param target the annotation container to store the mark in
     * @param elt the matched element
     * @param fromStubFileAnno the {@code @FromStubFile} mirror
     */
    private static void markFromStubFile(
            AnnotationFileAnnotations target, Element elt, AnnotationMirror fromStubFileAnno) {
        AnnotationMirrorSet marker = new AnnotationMirrorSet();
        marker.add(fromStubFileAnno);
        mergeDeclAnnos(
                target, ElementUtils.getQualifiedName(elt), marker, /* fromLazyJdk= */ false);
    }

    /**
     * Returns the subset of {@code annos} whose {@code @Target} meta-annotation permits use on an
     * element of the given kind. The text parser applies the same filter before recording a
     * declaration annotation (see {@code AnnotationFileParser.recordDeclAnnotation}); e.g. a {@code
     * TYPE_USE}-only annotation written before a class declaration is applied to the class's type
     * but not recorded as a declaration annotation of the class.
     *
     * @param annos the annotations to filter
     * @param kind the kind of the annotated element
     * @return the annotations applicable to {@code kind}; may be the empty set
     */
    private static AnnotationMirrorSet filterApplicable(
            AnnotationMirrorSet annos, ElementKind kind) {
        AnnotationMirrorSet result = new AnnotationMirrorSet();
        for (AnnotationMirror am : annos) {
            Target targetMeta = am.getAnnotationType().asElement().getAnnotation(Target.class);
            if (AnnotationUtils.getElementKindsForTarget(targetMeta).contains(kind)) {
                result.add(am);
            }
        }
        return result;
    }

    /**
     * Applies class type-parameter annotations from a {@link BinaryStubData.ClassRecord}. Stores
     * each annotated {@link AnnotatedTypeVariable} keyed by its {@link TypeParameterElement}, and
     * also stores the full {@link AnnotatedDeclaredType} (whose type arguments are those same type
     * variables) keyed by the {@link TypeElement}. This matches what {@link
     * AnnotationFileParser#processType} stores — the text parser stores the identical {@code
     * AnnotatedTypeVariable} objects under both keys — enabling {@code fromElement} to pick up the
     * annotations for both the class and its type parameters.
     *
     * <p>Uses {@link AnnotatedTypeMirror#createType} rather than {@code fromElement} to avoid
     * populating the {@code elementCache} prematurely (which would prevent later stub annotations
     * from being picked up).
     *
     * @param cr the class record from the binary stub data
     * @param className the fully-qualified class name
     * @param typeElt the element of the class
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param data the complete binary stub data
     * @param target the annotation container to store the results in
     * @param fromStubFileAnno the {@code @FromStubFile} mirror if this is a built-in stub file, or
     *     {@code null} for the lazily-loaded JDK; see {@code applyClassRecord}'s class-level
     *     comment for why this decides store-vs-merge-vs-skip for a pre-existing entry
     */
    private static void applyClassTypeParams(
            BinaryStubData.ClassRecord cr,
            String className,
            TypeElement typeElt,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data,
            AnnotationFileAnnotations target,
            @Nullable AnnotationMirror fromStubFileAnno) {
        List<? extends TypeParameterElement> typeParamElts = typeElt.getTypeParameters();

        // Build the AnnotatedDeclaredType using createType (avoids elementCache).
        AnnotatedDeclaredType declType =
                (AnnotatedDeclaredType)
                        AnnotatedTypeMirror.createType(typeElt.asType(), atypeFactory, false);
        List<? extends AnnotatedTypeMirror> typeArgs = declType.getTypeArguments();

        for (int i = 0;
                i < cr.typeParams.length && i < typeParamElts.size() && i < typeArgs.size();
                i++) {
            if (!(typeArgs.get(i) instanceof AnnotatedTypeVariable)) {
                continue;
            }
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) typeArgs.get(i);
            applyTypeParamRecord(
                    cr.typeParams[i], atv, className, data, atypeFactory, elementTypes);
            // Store keyed by TypeParameterElement — this is what
            // fromElement(TypeParameterElement) reads via stubTypes.getAnnotatedTypeMirror(tpe).
            // The same object is a type argument of declType, matching the text parser.
            storeOrMerge(target, typeParamElts.get(i), atv, atypeFactory, fromStubFileAnno);
        }

        // Store the full declared type keyed by TypeElement — this is what
        // fromElement(TypeElement) reads via stubTypes.getAnnotatedTypeMirror(typeElt).
        storeOrMerge(target, typeElt, declType, atypeFactory, fromStubFileAnno);
    }

    /**
     * Applies method type-parameter annotations to the type variables of {@code aet}, in place, and
     * stores each annotated type variable keyed by its {@link TypeParameterElement}, matching
     * {@code AnnotationFileParser.annotateTypeParameters} (which calls {@code putMerge(atypes,
     * paramType.getUnderlyingType().asElement(), paramType)}).
     *
     * @param typeParams the type-parameter records
     * @param aet the annotated executable type whose type variables to annotate
     * @param ee the element of the method or constructor
     * @param className the fully-qualified name of the enclosing class
     * @param data the complete binary stub data
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @param target the annotation container to store the results in
     * @param fromStubFileAnno the {@code @FromStubFile} mirror if this is a built-in stub file, or
     *     {@code null} for the lazily-loaded JDK; see {@code applyClassRecord}'s class-level
     *     comment for why this decides store-vs-merge-vs-skip for a pre-existing entry
     */
    private static void applyMethodTypeParams(
            BinaryStubData.TypeParamRecord[] typeParams,
            AnnotatedExecutableType aet,
            ExecutableElement ee,
            String className,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            AnnotationFileAnnotations target,
            @Nullable AnnotationMirror fromStubFileAnno) {
        List<? extends AnnotatedTypeMirror> typeVars = aet.getTypeVariables();
        List<? extends TypeParameterElement> typeParamElts = ee.getTypeParameters();
        for (int i = 0; i < typeParams.length && i < typeVars.size(); i++) {
            if (!(typeVars.get(i) instanceof AnnotatedTypeVariable)) {
                continue;
            }
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) typeVars.get(i);
            applyTypeParamRecord(typeParams[i], atv, className, data, atypeFactory, elementTypes);
            if (i < typeParamElts.size()) {
                storeOrMerge(target, typeParamElts.get(i), atv, atypeFactory, fromStubFileAnno);
            }
        }
    }

    /**
     * Applies one type-parameter record to an annotated type variable, matching the semantics of
     * {@code AnnotationFileParser.annotateTypeParameters}:
     *
     * <ul>
     *   <li>If the type parameter declares no bounds (e.g. {@code <@X T>}), annotations on the type
     *       variable itself become its primary annotation, covering both bounds.
     *   <li>Otherwise, annotations on the type variable apply to the lower bound, and if exactly
     *       one declared bound carries annotations, those are applied to the upper bound after
     *       clearing its existing annotations (matching {@code clearAnnotations} in the text
     *       parser's {@code annotate}). If more than one bound carries annotations, none are
     *       applied — the text parser has the same (unimplemented) intersection-bound limitation.
     * </ul>
     *
     * @param tp the type-parameter record to apply
     * @param atv the annotated type variable to annotate, modified in place
     * @param className the fully-qualified name of the enclosing class
     * @param data the complete binary stub data
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes per-factory state, including the annotation-mirror cache
     */
    private static void applyTypeParamRecord(
            BinaryStubData.TypeParamRecord tp,
            AnnotatedTypeVariable atv,
            String className,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        boolean hasDeclaredBounds = tp.boundAnnos.length > 0;
        for (int idx : tp.typeVarAnnos) {
            AnnotationMirror am =
                    annotationFromPool(idx, className, data, atypeFactory, elementTypes);
            if (am != null) {
                if (hasDeclaredBounds) {
                    atv.getLowerBound().replaceAnnotation(am);
                } else {
                    atv.replaceAnnotation(am);
                }
            }
        }

        // Find the single bound that carries annotations, if any.
        int annotatedBound = -1;
        for (int b = 0; b < tp.boundAnnos.length; b++) {
            if (tp.boundAnnos[b].length > 0) {
                if (annotatedBound >= 0) {
                    // More than one annotated bound: not supported, matching the text parser.
                    return;
                }
                annotatedBound = b;
            }
        }
        if (annotatedBound >= 0) {
            atv.getUpperBound().clearAnnotations();
            applyTypeAnnos(
                    atv.getUpperBound(),
                    tp.boundAnnos[annotatedBound],
                    className,
                    data,
                    atypeFactory,
                    elementTypes);
        }
    }

    /**
     * Propagates class type-parameter bound annotations from {@code target.atypes} to any {@link
     * AnnotatedTypeVariable} instances within {@code aet}. This is needed because when {@code aet}
     * is built via {@link AnnotatedTypeMirror#createType}, type variables like {@code K} and {@code
     * V} in {@code put(K key, V value)} do not yet carry the bounds stored for their {@link
     * TypeParameterElement} keys (e.g. {@code @NonNull Object} for {@code V}).
     *
     * @param aet the annotated executable type to update
     * @param target the annotation container holding the stored type-parameter entries
     */
    private static void propagateClassTypeParamBounds(
            AnnotatedExecutableType aet, AnnotationFileAnnotations target) {
        for (AnnotatedTypeMirror pType : aet.getParameterTypes()) {
            copyBoundsFromAtypes(pType, target);
        }
        copyBoundsFromAtypes(aet.getReturnType(), target);
        if (aet.getReceiverType() != null) {
            copyBoundsFromAtypes(aet.getReceiverType(), target);
        }
    }

    /**
     * If {@code atm} is an {@link AnnotatedTypeVariable} whose element is stored in {@code
     * target.atypes}, copies the bound annotations from the stored entry.
     *
     * @param atm the type to check
     * @param target the annotation container holding the stored type-parameter entries
     */
    private static void copyBoundsFromAtypes(
            AnnotatedTypeMirror atm, AnnotationFileAnnotations target) {
        if (!(atm instanceof AnnotatedTypeVariable)) {
            return;
        }
        AnnotatedTypeVariable atv = (AnnotatedTypeVariable) atm;
        Element tpeElt = atv.getUnderlyingType().asElement();
        if (tpeElt == null) {
            return;
        }
        AnnotatedTypeMirror stored = target.atypes.get(tpeElt);
        if (!(stored instanceof AnnotatedTypeVariable)) {
            return;
        }
        AnnotatedTypeVariable storedAtv = (AnnotatedTypeVariable) stored;
        for (AnnotationMirror am : storedAtv.getUpperBound().getAnnotations()) {
            atv.getUpperBound().replaceAnnotation(am);
        }
        for (AnnotationMirror am : storedAtv.getLowerBound().getAnnotations()) {
            atv.getLowerBound().replaceAnnotation(am);
        }
    }

    /**
     * Resolves one annotation-pool index into an {@link AnnotationMirror}. Returns {@code null} for
     * out-of-range indices and for annotations that cannot be resolved.
     *
     * @param idx an index into {@link BinaryStubData#annotationPool}
     * @param enclosingClassName fully-qualified name of the enclosing class, if any
     * @param data the complete binary stub data
     * @param atypeFactory the type factory of the currently-running checker
     * @param elementTypes per-factory state, including the annotation-mirror cache
     * @return the resolved annotation mirror, or {@code null} if it cannot be resolved
     */
    private static @Nullable AnnotationMirror annotationFromPool(
            int idx,
            String enclosingClassName,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        if (idx < 0 || idx >= data.annotationPool.length) {
            return null;
        }
        return getAnnotationMirror(
                data.annotationPool[idx], enclosingClassName, atypeFactory, data, elementTypes);
    }

    /**
     * Resolves a list of annotation-pool indices into a set of {@link AnnotationMirror}s. Indices
     * that cannot be resolved are silently skipped.
     *
     * @param annoPoolIndices indices into {@link BinaryStubData#annotationPool}
     * @param enclosingClassName fully-qualified name of the enclosing class, if any
     * @param data the binary stub data
     * @param atypeFactory the type factory of the currently-running checker
     * @param elementTypes per-factory state including the annotation mirror cache
     * @return the set of successfully resolved annotation mirrors
     */
    private static AnnotationMirrorSet parseDeclAnnos(
            int[] annoPoolIndices,
            String enclosingClassName,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        AnnotationMirrorSet set = new AnnotationMirrorSet();
        for (int idx : annoPoolIndices) {
            AnnotationMirror am =
                    annotationFromPool(idx, enclosingClassName, data, atypeFactory, elementTypes);
            if (am != null) {
                set.add(am);
            }
        }
        return set;
    }

    /**
     * Resolves a list of annotation-pool indices into the set of {@link AnnotationMirror}s whose
     * {@code @Target} permits use on an element of the given kind; the composition of {@link
     * #parseDeclAnnos} and {@link #filterApplicable}.
     *
     * @param annoPoolIndices indices into {@link BinaryStubData#annotationPool}
     * @param kind the kind of the annotated element
     * @param enclosingClassName fully-qualified name of the enclosing class, if any
     * @param data the binary stub data
     * @param atypeFactory the type factory of the currently-running checker
     * @param elementTypes per-factory state including the annotation mirror cache
     * @return the resolved annotation mirrors applicable to {@code kind}; may be the empty set
     */
    private static AnnotationMirrorSet parseApplicableDeclAnnos(
            int[] annoPoolIndices,
            ElementKind kind,
            String enclosingClassName,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        return filterApplicable(
                parseDeclAnnos(
                        annoPoolIndices, enclosingClassName, data, atypeFactory, elementTypes),
                kind);
    }

    /**
     * Applies a list of type annotations to {@code atm}, navigating each annotation's type path to
     * locate the annotated component. Uses {@link AnnotatedTypeMirror#replaceAnnotation}, so an
     * existing annotation from the same hierarchy is replaced rather than duplicated, and
     * annotations that are not supported qualifiers of the current checker are filtered out.
     *
     * <p>Unlike the text parser, this does not copy type-variable bound annotations into implicit
     * (unbounded, unannotated) wildcards: the text parser bakes the result of a parse-time {@code
     * getAnnotatedType} call into the stored type, which is derived (and order-dependent) state
     * that {@code mergeAnnotationFileAnnosIntoType}'s target recomputes at lookup time anyway.
     *
     * @param atm the annotated type mirror to annotate
     * @param annos the type annotations to apply
     * @param enclosingClassName fully-qualified name of the enclosing class, if any
     * @param data the binary stub data
     * @param atypeFactory the type factory of the currently-running checker
     * @param elementTypes per-factory state, including the annotation-mirror cache
     */
    private static void applyTypeAnnos(
            AnnotatedTypeMirror atm,
            BinaryStubData.TypeAnno[] annos,
            String enclosingClassName,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        for (BinaryStubData.TypeAnno ta : annos) {
            AnnotatedTypeMirror component = resolvePath(atm, ta.path);
            if (component == null) {
                continue;
            }
            AnnotationMirror am =
                    annotationFromPool(
                            ta.annoIndex, enclosingClassName, data, atypeFactory, elementTypes);
            if (am != null) {
                component.replaceAnnotation(am);
            }
        }
    }

    /**
     * Navigates a type-annotation path starting from {@code atm} and returns the component type
     * reached after following all steps in {@code path}. Returns {@code null} if the path is
     * invalid for the given type.
     *
     * @param atm the starting annotated type mirror
     * @param path the sequence of type-path steps
     * @return the annotated type mirror at the end of the path, or {@code null} if unreachable
     */
    private static AnnotatedTypeMirror resolvePath(
            AnnotatedTypeMirror atm, BinaryStubData.TypePathStep[] path) {
        AnnotatedTypeMirror current = atm;
        for (BinaryStubData.TypePathStep step : path) {
            if (current == null) {
                return null;
            }
            if (step.kind == 0) { // ARRAY
                if (current instanceof AnnotatedArrayType) {
                    current = ((AnnotatedArrayType) current).getComponentType();
                } else {
                    return null;
                }
            } else if (step.kind == 2) { // WILDCARD_BOUND
                if (current instanceof AnnotatedWildcardType) {
                    AnnotatedWildcardType awt = (AnnotatedWildcardType) current;
                    // CF's AnnotatedWildcardType always synthesizes both bounds (defaulting
                    // whichever was not written in source), so getExtendsBound() is never null
                    // and cannot be used to detect which bound this step is for; argIndex carries
                    // that distinction instead (0 = extends, 1 = super -- see BinaryStubWriter's
                    // extractTypeAnnotations, which writes it for exactly this reason).
                    current = step.argIndex == 0 ? awt.getExtendsBound() : awt.getSuperBound();
                } else {
                    return null;
                }
            } else if (step.kind == 3) { // TYPE_ARGUMENT
                if (current instanceof AnnotatedDeclaredType) {
                    AnnotatedDeclaredType adt = (AnnotatedDeclaredType) current;
                    // argIndex is stored as a signed byte (see TypePathStep#argIndex); widen it
                    // back to its unsigned (0-255) meaning, matching JVMS's u1
                    // type_argument_index, rather than sign-extending a value of 128 or greater
                    // to a negative number.
                    int argIndex = Byte.toUnsignedInt(step.argIndex);
                    if (argIndex < adt.getTypeArguments().size()) {
                        current = adt.getTypeArguments().get(argIndex);
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                // Kind 1 (nested type) is never written, so it is never resolved; see
                // BinaryStubData.TypePathStep#kind. Any other value is malformed.
                return null;
            }
        }
        return current;
    }

    /**
     * Resolves a structural {@link BinaryStubData.AnnotationRecord} into an {@link
     * AnnotationMirror}. Results are memoised in the compilation-wide cache held by {@code
     * elementTypes}, except for records containing {@link BinaryStubData.NameLiteralValue}s, whose
     * resolution depends on the enclosing class. Annotations whose type is not on the
     * annotation-processor classpath are silently skipped.
     *
     * <p>A failure to resolve is memoised too, as a null value: {@code computeIfAbsent} does not
     * record a null result, so an unresolvable record would be rebuilt on every one of its
     * occurrences -- rethrowing and refilling a stack trace that is immediately discarded, and
     * re-reporting {@code createAnnotationMirrorNoCache}'s warning once per occurrence rather than
     * once per record. Nothing in the annotated JDK or in the built-in stub files is unresolvable
     * today: the JDK-internal {@code @DefinedBy}, {@code @IntrinsicCandidate}, and
     * {@code @CallerSensitive} all resolve through {@code Elements.getTypeElement} even though
     * their packages are not exported. So this costs one extra {@code containsKey} on a path
     * nothing currently takes; it is here so the memoisation is not silently wrong for the first
     * annotation that does fail.
     *
     * @param ar the structural annotation record to deserialize
     * @param enclosingClassName the fully-qualified name of the enclosing class
     * @param atypeFactory the type factory of the currently-running checker
     * @param data the complete binary stub data, providing the string pool
     * @param elementTypes per-factory state including the annotation mirror cache
     * @return the resolved annotation mirror, or {@code null} if it cannot be resolved
     */
    private static @Nullable AnnotationMirror getAnnotationMirror(
            BinaryStubData.AnnotationRecord ar,
            String enclosingClassName,
            AnnotatedTypeFactory atypeFactory,
            BinaryStubData data,
            AnnotationFileElementTypes elementTypes) {
        if (ar.hasNameLiteral) {
            return createAnnotationMirrorNoCache(
                    ar, enclosingClassName, atypeFactory, data, elementTypes);
        }
        AnnotationFileElementTypes.BinaryStubDataCache cache =
                elementTypes.getBinaryStubDataCache();
        if (cache == null) {
            return createAnnotationMirrorNoCache(
                    ar, enclosingClassName, atypeFactory, data, elementTypes);
        }
        // Not computeIfAbsent: it does not record a null result, so an unresolvable annotation
        // would be recomputed -- and rethrow -- on every one of its occurrences. One map lookup on
        // the hot (resolved) path; the containsKey call is reached only for a null value.
        AnnotationMirror cached = cache.annoCache.get(ar);
        if (cached != null || cache.annoCache.containsKey(ar)) {
            return cached;
        }
        AnnotationMirror am =
                createAnnotationMirrorNoCache(
                        ar, enclosingClassName, atypeFactory, data, elementTypes);
        cache.annoCache.put(ar, am);
        return am;
    }

    /**
     * Dispatches a scalar annotation-member value to the appropriate {@link
     * AnnotationBuilder#setValue} overload.
     *
     * @param builder the annotation builder to dispatch the value to
     * @param name the annotation element name
     * @param val the value to dispatch; must be a type handled by {@link
     *     AnnotationBuilder#setValue}
     */
    private static void dispatchSetValue(AnnotationBuilder builder, String name, Object val) {
        if (val instanceof Boolean) {
            builder.setValue(name, (Boolean) val);
        } else if (val instanceof Character) {
            builder.setValue(name, (Character) val);
        } else if (val instanceof Double) {
            builder.setValue(name, (Double) val);
        } else if (val instanceof Float) {
            builder.setValue(name, (Float) val);
        } else if (val instanceof Integer) {
            builder.setValue(name, (Integer) val);
        } else if (val instanceof Long) {
            builder.setValue(name, (Long) val);
        } else if (val instanceof Short) {
            builder.setValue(name, (Short) val);
        } else if (val instanceof Byte) {
            // AnnotationBuilder has no Byte overload; a byte member accepts a Short value.
            builder.setValue(name, (Short) ((Byte) val).shortValue());
        } else if (val instanceof String) {
            builder.setValue(name, (String) val);
        } else if (val instanceof TypeMirror) {
            builder.setValue(name, (TypeMirror) val);
        } else if (val instanceof VariableElement) {
            builder.setValue(name, (VariableElement) val);
        } else if (val instanceof AnnotationMirror) {
            builder.setValue(name, (AnnotationMirror) val);
        }
    }

    /**
     * Recursively looks up a field with the given simple name in a TypeElement, its superclasses,
     * and its interfaces. Used to resolve simple-name constant references in annotation values
     * (e.g. {@code @IntRange(from = MIN_CODE_POINT)}).
     *
     * @param te the type element to search
     * @param name the simple name of the field to find
     * @param env the processing environment
     * @return the resolved variable element, or {@code null} if not found
     */
    static VariableElement findFieldInType(TypeElement te, String name, ProcessingEnvironment env) {
        if (te == null) {
            return null;
        }
        for (Element elt : te.getEnclosedElements()) {
            if (elt.getKind() == ElementKind.FIELD) {
                VariableElement ve = (VariableElement) elt;
                if (ve.getSimpleName().contentEquals(name)) {
                    return ve;
                }
            }
        }
        if (te.getSuperclass().getKind() == TypeKind.DECLARED) {
            Element superElt = env.getTypeUtils().asElement(te.getSuperclass());
            if (superElt instanceof TypeElement) {
                VariableElement ve = findFieldInType((TypeElement) superElt, name, env);
                if (ve != null) {
                    return ve;
                }
            }
        }
        for (TypeMirror itf : te.getInterfaces()) {
            if (itf.getKind() == TypeKind.DECLARED) {
                Element itfElt = env.getTypeUtils().asElement(itf);
                if (itfElt instanceof TypeElement) {
                    VariableElement ve = findFieldInType((TypeElement) itfElt, name, env);
                    if (ve != null) {
                        return ve;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolves a single structural value item, converting it to the expected element type where
     * necessary.
     *
     * @param val the value object to resolve
     * @param expectedKind the expected type kind of the annotation member (for an array member, the
     *     kind of its component type)
     * @param enclosingClassName the fully-qualified name of the enclosing class
     * @param atypeFactory the type factory of the currently-running checker
     * @param data the complete binary stub data, providing the string pool
     * @param elementTypes per-factory state including the annotation mirror cache
     * @return the resolved object representing the annotation value, or {@code null} if it cannot
     *     be resolved
     */
    private static Object resolveSingleValue(
            Object val,
            TypeKind expectedKind,
            String enclosingClassName,
            AnnotatedTypeFactory atypeFactory,
            BinaryStubData data,
            AnnotationFileElementTypes elementTypes) {
        ProcessingEnvironment env = atypeFactory.getProcessingEnv();
        if (val instanceof Boolean || val instanceof String) {
            return val;
        } else if (val instanceof Character || val instanceof Number) {
            // The writer stores every integral literal as a long and every floating-point literal
            // as a double; convert to the kind the annotation member declares.
            return coerceToKind(val, expectedKind);
        } else if (val instanceof BinaryStubData.ClassLiteralValue) {
            String fqName = ((BinaryStubData.ClassLiteralValue) val).className;
            AnnotationFileElementTypes.BinaryStubDataCache cache =
                    elementTypes.getBinaryStubDataCache();
            if (cache != null) {
                TypeMirror cachedType = cache.resolvedClassTypesCache.get(fqName);
                if (cachedType != null) {
                    return cachedType;
                }
            }
            @SuppressWarnings("signature:argument.type.incompatible") // fqName is read from the
            // binary stub's string pool, which BinaryStubWriter populates only with
            // fully-qualified names
            TypeElement te = env.getElementUtils().getTypeElement(fqName);
            if (te == null) {
                return null;
            }
            TypeMirror type = te.asType();
            if (cache != null) {
                cache.resolvedClassTypesCache.put(fqName, type);
            }
            return type;
        } else if (val instanceof BinaryStubData.EnumConstantValue) {
            // Despite the name, the writer tags every FieldAccessExpr this way (see
            // BinaryStubWriter#writeValue), so the referenced member need not be an enum
            // constant: e.g. "@IntRange(to = Integer.MAX_VALUE)" refers to a plain static final
            // field. Mirror AnnotationFileParser#getValueOfExpressionInAnnotation, which resolves
            // the field first and only then asks for its constant value (an enum constant has
            // none, so the element itself is used; a constant field's value is used and coerced).
            BinaryStubData.EnumConstantValue ev = (BinaryStubData.EnumConstantValue) val;
            String cacheKey = ev.enumClassName + "#" + ev.constantName;
            AnnotationFileElementTypes.BinaryStubDataCache cache =
                    elementTypes.getBinaryStubDataCache();
            if (cache != null) {
                Object cachedVal = cache.resolvedConstantsCache.get(cacheKey);
                if (cachedVal != null) {
                    // coerceToKind is a no-op for a cached enum-constant VariableElement; it only
                    // converts a cached constant field's numeric/character value.
                    return coerceToKind(cachedVal, expectedKind);
                }
            }
            @SuppressWarnings("signature:argument.type.incompatible") // ev.enumClassName is read
            // from the binary stub's string pool, which BinaryStubWriter populates only with
            // fully-qualified names
            TypeElement enumClass = env.getElementUtils().getTypeElement(ev.enumClassName);
            if (enumClass != null) {
                for (Element elt : enumClass.getEnclosedElements()) {
                    if (elt.getKind() == ElementKind.ENUM_CONSTANT
                            && elt.getSimpleName().contentEquals(ev.constantName)) {
                        if (cache != null) {
                            cache.resolvedConstantsCache.put(cacheKey, elt);
                        }
                        return elt;
                    }
                }
                VariableElement fieldElt = findFieldInType(enumClass, ev.constantName, env);
                if (fieldElt != null) {
                    Object cVal = fieldElt.getConstantValue();
                    if (cVal != null) {
                        if (cache != null) {
                            cache.resolvedConstantsCache.put(cacheKey, cVal);
                        }
                        return coerceToKind(cVal, expectedKind);
                    }
                }
            }
        } else if (val instanceof BinaryStubData.NameLiteralValue) {
            // A simple-name reference to a constant in the enclosing class (or a supertype).
            String constantName = ((BinaryStubData.NameLiteralValue) val).name;
            if (enclosingClassName != null) {
                String cacheKey = enclosingClassName + "#" + constantName;
                AnnotationFileElementTypes.BinaryStubDataCache cache =
                        elementTypes.getBinaryStubDataCache();
                if (cache != null) {
                    Object cachedVal = cache.resolvedConstantsCache.get(cacheKey);
                    if (cachedVal != null) {
                        return coerceToKind(cachedVal, expectedKind);
                    }
                }
                @SuppressWarnings("signature:argument.type.incompatible") // enclosingClassName is
                // read from the binary stub's string pool, which BinaryStubWriter populates only
                // with fully-qualified names
                TypeElement te = env.getElementUtils().getTypeElement(enclosingClassName);
                VariableElement ve = findFieldInType(te, constantName, env);
                if (ve != null) {
                    Object cVal = ve.getConstantValue();
                    if (cVal != null) {
                        if (cache != null) {
                            cache.resolvedConstantsCache.put(cacheKey, cVal);
                        }
                        return coerceToKind(cVal, expectedKind);
                    }
                }
            }
            // Could not resolve — skip silently, matching AnnotationFileParser behavior.
            return null;
        } else if (val instanceof BinaryStubData.AnnotationRecord) {
            return getAnnotationMirror(
                    (BinaryStubData.AnnotationRecord) val,
                    enclosingClassName,
                    atypeFactory,
                    data,
                    elementTypes);
        }
        return null;
    }

    /**
     * Resolves a structural annotation member value and adds it to the annotation builder.
     *
     * @param builder the annotation builder to add the value to
     * @param name the member element name
     * @param val the serialized value object
     * @param enclosingClassName the fully-qualified name of the enclosing class
     * @param atypeFactory the type factory of the currently-running checker
     * @param data the complete binary stub data, providing the string pool
     * @param elementTypes per-factory state including the annotation mirror cache
     * @throws BugInCF if the annotation type declares no member named {@code name}, or if the
     *     resolved value does not fit that member's declared type
     */
    @SuppressWarnings("unchecked")
    private static void addValueToBuilder(
            AnnotationBuilder builder,
            String name,
            Object val,
            String enclosingClassName,
            AnnotatedTypeFactory atypeFactory,
            BinaryStubData data,
            AnnotationFileElementTypes elementTypes) {
        // findElement throws BugInCF if the annotation type declares no such member, which the
        // caller turns into a skipped annotation. Letting it escape here rather than catching it
        // loses nothing: every path below reaches AnnotationBuilder.setValue, whose own
        // findElement call would throw the same exception a moment later.
        ExecutableElement elem = builder.findElement(name);
        TypeMirror returnType = elem.getReturnType();
        boolean isArray = returnType.getKind() == TypeKind.ARRAY;
        TypeKind expectedKind =
                isArray
                        ? ((ArrayType) returnType).getComponentType().getKind()
                        : returnType.getKind();

        if (isArray) {
            if (val instanceof List) {
                List<Object> rawList = (List<Object>) val;
                List<Object> resolvedList = new ArrayList<>(rawList.size());
                for (Object item : rawList) {
                    Object resolved =
                            resolveSingleValue(
                                    item,
                                    expectedKind,
                                    enclosingClassName,
                                    atypeFactory,
                                    data,
                                    elementTypes);
                    if (resolved != null) {
                        resolvedList.add(resolved);
                    }
                }
                builder.setValue(name, resolvedList);
            } else {
                // A single value for an array member denotes a one-element array.
                Object resolved =
                        resolveSingleValue(
                                val,
                                expectedKind,
                                enclosingClassName,
                                atypeFactory,
                                data,
                                elementTypes);
                if (resolved != null) {
                    builder.setValue(name, Collections.singletonList(resolved));
                }
            }
        } else {
            Object resolved =
                    resolveSingleValue(
                            val,
                            expectedKind,
                            enclosingClassName,
                            atypeFactory,
                            data,
                            elementTypes);
            if (resolved != null) {
                dispatchSetValue(builder, name, resolved);
            }
        }
    }

    /**
     * Converts a numeric or character annotation-member value to the primitive kind the member
     * declares, mirroring {@code AnnotationFileParser.convert} (which the text parser applies to
     * every integer, long, and character literal, and to every resolved numeric constant).
     *
     * <p>Both of this method's callers need every primitive kind handled, including {@code FLOAT}
     * and {@code DOUBLE}. Widening an integral literal to a floating-point member is ordinary Java
     * ({@code @Anno(0)} for {@code double value();}), and the writer records that {@code 0} as a
     * long; returning it unconverted makes {@code AnnotationBuilder.setValue} reject an {@code
     * Integer} where a {@code Double} is expected, which {@code createAnnotationMirrorNoCache}
     * turns into a silently dropped annotation. The same applies to a character literal or constant
     * used for a numeric member ({@code @Anno('a')} for {@code int value();}), which the text
     * parser converts through {@code (int) charValue}.
     *
     * <p>A value whose kind this method does not convert (e.g. a {@code String} constant, or a
     * numeric value for a member whose kind could not be determined) is returned unchanged.
     *
     * @param value the raw value: a {@code Character}, or a {@code Number} as recorded by the
     *     writer or read from a resolved constant field
     * @param expectedKind the type kind of the annotation member (for an array member, of its
     *     component type)
     * @return the converted value, or {@code value} itself if no conversion applies
     */
    static Object coerceToKind(Object value, TypeKind expectedKind) {
        Number number;
        if (value instanceof Character) {
            number = (int) ((Character) value).charValue();
        } else if (value instanceof Number) {
            number = (Number) value;
        } else {
            return value;
        }
        switch (expectedKind) {
            case BYTE:
                return number.byteValue();
            case SHORT:
                return number.shortValue();
            case INT:
                return number.intValue();
            case LONG:
                return number.longValue();
            case CHAR:
                return (char) number.longValue();
            case FLOAT:
                return number.floatValue();
            case DOUBLE:
                return number.doubleValue();
            default:
                // The member's kind is not a numeric primitive -- e.g. it could not be determined
                // (TypeKind.NONE, see addValueToBuilder). Leave the value alone; setValue will
                // reject it and the annotation is skipped, as before.
                return value;
        }
    }

    /**
     * Creates an AnnotationMirror structurally, bypassing the annotation cache.
     *
     * @param ar the structural annotation record to deserialize
     * @param enclosingClassName the fully-qualified name of the enclosing class
     * @param atypeFactory the type factory of the currently-running checker
     * @param data the complete binary stub data, providing the string pool
     * @param elementTypes per-factory state including the annotation mirror cache
     * @return the resolved annotation mirror, or {@code null} if it cannot be resolved
     */
    private static @Nullable AnnotationMirror createAnnotationMirrorNoCache(
            BinaryStubData.AnnotationRecord ar,
            String enclosingClassName,
            AnnotatedTypeFactory atypeFactory,
            BinaryStubData data,
            AnnotationFileElementTypes elementTypes) {
        String fqn = data.stringPool[ar.nameIndex];
        AnnotationBuilder builder;
        try {
            @SuppressWarnings("signature:argument.type.incompatible") // fqn is read from the
            // binary stub's string pool, which BinaryStubWriter populates only with
            // fully-qualified names
            AnnotationBuilder b = new AnnotationBuilder(atypeFactory.getProcessingEnv(), fqn);
            builder = b;
        } catch (UserError e) {
            // The annotation's type is not on the annotation-processor classpath -- e.g. a
            // JDK-internal annotation such as @DefinedBy, which the annotated JDK's stub sources
            // carry but no checker can see. AnnotationBuilder's constructor is the only place this
            // is detected, and it is the sole expected failure of this method: skip the annotation
            // silently, matching AnnotationFileParser.
            return null;
        }

        try {
            for (Map.Entry<Integer, Object> entry : ar.elementValues.entrySet()) {
                String memberName = data.stringPool[entry.getKey()];
                addValueToBuilder(
                        builder,
                        memberName,
                        entry.getValue(),
                        enclosingClassName,
                        atypeFactory,
                        data,
                        elementTypes);
            }
            return builder.build();
        } catch (BugInCF e) {
            // The annotation's type resolved, but one of its element values did not fit the
            // member it was written for -- a bug in this class or in BinaryStubWriter, not a
            // condition a stub file can legitimately produce. Skip the annotation rather than
            // aborting the compilation, but say so: the previous catch of every Exception made
            // such a bug indistinguishable from the expected UserError above, and hid a reader
            // that could not widen an integral literal to a floating-point member. Anything other
            // than a BugInCF (a NullPointerException, say) is a defect with no recovery story and
            // now propagates.
            atypeFactory
                    .getChecker()
                    .message(
                            Diagnostic.Kind.WARNING,
                            "Binary stub: could not build @"
                                    + fqn
                                    + (enclosingClassName == null
                                            ? ""
                                            : " in " + enclosingClassName)
                                    + "; skipping it. This is a Checker Framework bug: "
                                    + e.getMessage());
            return null;
        }
    }
}
