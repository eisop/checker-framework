package org.checkerframework.framework.stub;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.plumelib.util.IPair;

import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * #applyFakeOverride}). Classes that are not present in the binary stub (record declarations are
 * the one construct {@code BinaryStubWriter} does not write) fall through to the text-based {@link
 * AnnotationFileParser} path.
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
            AnnotationMirrorSet pkgDeclAnnos =
                    parseApplicableDeclAnnos(
                            entry.getValue(),
                            ElementKind.PACKAGE,
                            null,
                            data,
                            atypeFactory,
                            elementTypes);
            if (!pkgDeclAnnos.isEmpty()) {
                mergeDeclAnnos(target, entry.getKey(), pkgDeclAnnos, fromLazyJdk);
            }
        }
        for (Map.Entry<String, int[]> entry : data.modules.entrySet()) {
            AnnotationMirrorSet modDeclAnnos =
                    parseApplicableDeclAnnos(
                            entry.getValue(),
                            ElementKind.MODULE,
                            null,
                            data,
                            atypeFactory,
                            elementTypes);
            if (!modDeclAnnos.isEmpty()) {
                mergeDeclAnnos(target, entry.getKey(), modDeclAnnos, fromLazyJdk);
            }
        }
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

        // Whether a type is already stored for this class BEFORE any of this class record's own
        // processing runs -- e.g. because a user-supplied stub file (-Astubs) was parsed earlier
        // and already established Optional's type. Captured up front, since applyClassTypeParams
        // below may itself populate target.atypes[typeElt] (via putIfAbsent) once this method
        // starts running; testing containsKey() again afterwards would then always be true and
        // could never detect the JDK-vs-user-stub case this guards against.
        boolean typeAlreadyPresent = target.atypes.containsKey(typeElt);

        // Apply class declaration annotations, merged by name with any existing entry. Only
        // annotations whose @Target permits use on this kind of declaration are recorded as
        // declaration annotations; TYPE_USE-only annotations are applied to the class's type
        // below instead.
        AnnotationMirrorSet classDeclAnnos =
                parseDeclAnnos(cr.declAnnos, className, data, atypeFactory, elementTypes);
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
        if (cr.typeParams.length > 0) {
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
                boolean alreadyPresent = target.atypes.containsKey(ve);
                if (!alreadyPresent || fromStubFileAnno != null) {
                    // Build fresh either way (matches applyClassRecord's class-level merge):
                    // if nothing is stored yet, this becomes the stored type; if something from
                    // an earlier built-in stub file is already stored, this built-in stub file's
                    // annotations replace it (last stub file wins), but never the reverse -- see
                    // applyClassRecord's class-level-annotation comment for the full rationale.
                    AnnotatedTypeMirror atm =
                            AnnotatedTypeMirror.createType(ve.asType(), atypeFactory, false);
                    applyTypeAnnos(atm, fr.typeAnnos, className, data, atypeFactory, elementTypes);
                    if (!alreadyPresent) {
                        target.atypes.put(ve, atm);
                    } else {
                        atypeFactory.replaceAnnotations(atm, target.atypes.get(ve));
                    }
                }
                // else: fromStubFileAnno == null (the lazily-loaded JDK) and an entry already
                // exists from an earlier source -- leave it untouched.
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
            ExecutableElement ee =
                    sig.startsWith("<init>(")
                            ? elementTypes.constructorSigIndex(typeElt).get(sig)
                            : elementTypes.methodSigIndex(typeElt).get(sig);
            if (ee == null) {
                // Not declared in this class. The stub may declare a "fake override":
                // annotations on an inherited method that apply only at this subtype (see
                // AnnotationFileParser.processFakeOverride). Otherwise the method does not
                // exist in the JDK being compiled against and there is nothing to apply.
                if (!sig.startsWith("<init>(")) {
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

            if (aet.getReceiverType() != null && mr.receiverAnnos.length > 0) {
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
                        AnnotatedTypeMirror pInner = getInnermostComponentType(pType);
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
                    // the stored method type. As with the method's own aet (see below), a
                    // pre-existing entry from the lazily-loaded JDK is never touched, but one
                    // from an earlier built-in stub file is merged onto (last stub file wins).
                    if (!target.atypes.containsKey(pElt)) {
                        target.atypes.put(pElt, pType);
                    } else if (fromStubFileAnno != null) {
                        atypeFactory.replaceAnnotations(pType, target.atypes.get(pElt));
                    }
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
            // As with the field- and class-level cases (see applyClassRecord's class-level
            // comment): store fresh if nothing existed, merge onto an existing entry from an
            // earlier built-in stub file (last stub file wins), or -- methodAlreadyPresent &&
            // fromStubFileAnno == null -- this point is unreachable (the lazily-loaded JDK case
            // already backed off above).
            if (!methodAlreadyPresent) {
                target.atypes.put(ee, aet);
            } else {
                atypeFactory.replaceAnnotations(aet, target.atypes.get(ee));
            }
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
        TypeElement superClass = ElementUtils.getSuperClass(typeElt);
        if (superClass != null) {
            ExecutableElement found = elementTypes.methodSigIndex(superClass).get(sig);
            if (found == null) {
                found = findFakeOverriddenMethod(superClass, sig, elementTypes);
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
            if (!(interfaceElt instanceof TypeElement)) {
                continue;
            }
            ExecutableElement found =
                    elementTypes.methodSigIndex((TypeElement) interfaceElt).get(sig);
            if (found == null) {
                found = findFakeOverriddenMethod((TypeElement) interfaceElt, sig, elementTypes);
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
            // The same object is a type argument of declType, matching the text parser. As with
            // applyClassRecord's class-level annotations: store fresh if absent, merge onto an
            // existing entry from an earlier built-in stub file (last stub file wins), or leave
            // untouched if the existing entry is from a non-JDK source and this is the lazily
            // loaded JDK.
            TypeParameterElement tpe = typeParamElts.get(i);
            if (!target.atypes.containsKey(tpe)) {
                target.atypes.put(tpe, atv);
            } else if (fromStubFileAnno != null) {
                atypeFactory.replaceAnnotations(atv, target.atypes.get(tpe));
            }
        }

        // Store the full declared type keyed by TypeElement — this is what
        // fromElement(TypeElement) reads via stubTypes.getAnnotatedTypeMirror(typeElt). Same
        // store/merge/skip decision as above.
        if (!target.atypes.containsKey(typeElt)) {
            target.atypes.put(typeElt, declType);
        } else if (fromStubFileAnno != null) {
            atypeFactory.replaceAnnotations(declType, target.atypes.get(typeElt));
        }
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
                TypeParameterElement tpe = typeParamElts.get(i);
                if (!target.atypes.containsKey(tpe)) {
                    target.atypes.put(tpe, atv);
                } else if (fromStubFileAnno != null) {
                    atypeFactory.replaceAnnotations(atv, target.atypes.get(tpe));
                }
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
     * Returns the innermost component type of {@code atm}, unwrapping array layers. For non-array
     * types, returns {@code atm} itself.
     *
     * @param atm the annotated type mirror to unwrap
     * @return the innermost non-array component type, or {@code atm} if it is not an array
     */
    private static AnnotatedTypeMirror getInnermostComponentType(AnnotatedTypeMirror atm) {
        AnnotatedTypeMirror current = atm;
        while (current.getKind() == TypeKind.ARRAY) {
            current = ((AnnotatedArrayType) current).getComponentType();
        }
        return current;
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
                    current =
                            awt.getExtendsBound() != null
                                    ? awt.getExtendsBound()
                                    : awt.getSuperBound();
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
     * @param ar the structural annotation record to deserialize
     * @param enclosingClassName the fully-qualified name of the enclosing class
     * @param atypeFactory the type factory of the currently-running checker
     * @param data the complete binary stub data, providing the string pool
     * @param elementTypes per-factory state including the annotation mirror cache
     * @return the resolved annotation mirror, or {@code null} if it cannot be resolved
     */
    private static AnnotationMirror getAnnotationMirror(
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
        return cache.annoCache.computeIfAbsent(
                ar,
                r ->
                        createAnnotationMirrorNoCache(
                                r, enclosingClassName, atypeFactory, data, elementTypes));
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
    private static VariableElement findFieldInType(
            TypeElement te, String name, ProcessingEnvironment env) {
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
        if (val instanceof Boolean || val instanceof Character || val instanceof String) {
            return val;
        } else if (val instanceof Long) {
            // The writer stores every integral literal as a long; narrow it to the kind the
            // annotation member declares.
            Long l = (Long) val;
            if (expectedKind == TypeKind.LONG) {
                return l;
            }
            if (expectedKind == TypeKind.SHORT) {
                return l.shortValue();
            }
            if (expectedKind == TypeKind.BYTE) {
                return l.byteValue();
            }
            if (expectedKind == TypeKind.CHAR) {
                return (char) l.longValue();
            }
            return l.intValue();
        } else if (val instanceof Double) {
            // The writer stores every floating-point literal as a double.
            Double d = (Double) val;
            if (expectedKind == TypeKind.FLOAT) {
                return d.floatValue();
            }
            return d;
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
            BinaryStubData.EnumConstantValue ev = (BinaryStubData.EnumConstantValue) val;
            @SuppressWarnings("signature:argument.type.incompatible") // ev.enumClassName is read
            // from the binary stub's string pool, which BinaryStubWriter populates only with
            // fully-qualified names
            TypeElement enumClass = env.getElementUtils().getTypeElement(ev.enumClassName);
            if (enumClass != null) {
                for (Element elt : enumClass.getEnclosedElements()) {
                    if (elt.getKind() == ElementKind.ENUM_CONSTANT
                            && elt.getSimpleName().contentEquals(ev.constantName)) {
                        return elt;
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
                        return coerceConstant(cachedVal, expectedKind);
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
                        return coerceConstant(cVal, expectedKind);
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
        boolean isArray = false;
        TypeKind expectedKind = TypeKind.NONE;
        try {
            ExecutableElement elem = builder.findElement(name);
            TypeMirror returnType = elem.getReturnType();
            isArray = returnType.getKind() == TypeKind.ARRAY;
            if (isArray) {
                expectedKind = ((ArrayType) returnType).getComponentType().getKind();
            } else {
                expectedKind = returnType.getKind();
            }
        } catch (Exception e) {
            // Silently ignore if the annotation element cannot be found.
        }

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
     * Coerces a raw constant value (Character or Number) to the expected type kind.
     *
     * @param cVal the raw constant value
     * @param expectedKind the expected type kind
     * @return the coerced value, or the original value if no coercion is needed
     */
    private static Object coerceConstant(Object cVal, TypeKind expectedKind) {
        if (cVal instanceof Character) {
            int charCode = (int) ((Character) cVal).charValue();
            if (expectedKind == TypeKind.LONG) {
                return (long) charCode;
            }
            if (expectedKind == TypeKind.SHORT) {
                return (short) charCode;
            }
            if (expectedKind == TypeKind.BYTE) {
                return (byte) charCode;
            }
            if (expectedKind == TypeKind.CHAR) {
                return (char) charCode;
            }
            return charCode;
        } else if (cVal instanceof Number) {
            Number n = (Number) cVal;
            if (expectedKind == TypeKind.LONG) {
                return n.longValue();
            }
            if (expectedKind == TypeKind.SHORT) {
                return n.shortValue();
            }
            if (expectedKind == TypeKind.BYTE) {
                return n.byteValue();
            }
            if (expectedKind == TypeKind.CHAR) {
                return (char) n.longValue();
            }
            return n.intValue();
        }
        return cVal;
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
    private static AnnotationMirror createAnnotationMirrorNoCache(
            BinaryStubData.AnnotationRecord ar,
            String enclosingClassName,
            AnnotatedTypeFactory atypeFactory,
            BinaryStubData data,
            AnnotationFileElementTypes elementTypes) {
        try {
            String fqn = data.stringPool[ar.nameIndex];
            @SuppressWarnings("signature:argument.type.incompatible") // fqn is read from the
            // binary stub's string pool, which BinaryStubWriter populates only with
            // fully-qualified names
            AnnotationBuilder builder = new AnnotationBuilder(atypeFactory.getProcessingEnv(), fqn);

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
        } catch (Exception e) {
            // Silently skip annotations that cannot be resolved (e.g. JDK-internal annotations
            // not on the annotation-processor classpath), matching AnnotationFileParser behavior.
            return null;
        }
    }
}
