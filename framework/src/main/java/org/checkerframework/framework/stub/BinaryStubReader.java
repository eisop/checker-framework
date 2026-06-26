package org.checkerframework.framework.stub;

import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;

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
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

/**
 * Applies annotation data from a {@link BinaryStubData.ClassRecord} to a checker's {@link
 * AnnotationFileAnnotations}. This translates the compact structural binary representation back
 * into {@link AnnotationMirror} and {@link AnnotatedTypeMirror} objects that the framework uses
 * during type-checking.
 *
 * <p>Annotations that cannot be resolved (e.g. JDK-internal annotations like {@code @DefinedBy}
 * that are absent from the annotation-processor classpath) are silently skipped, matching the
 * behavior of the text-based {@link AnnotationFileParser}.
 *
 * <p>User-supplied stub files take precedence: if an entry already exists in {@link
 * AnnotationFileAnnotations#atypes} or {@link AnnotationFileAnnotations#declAnnos} it is not
 * overwritten.
 *
 * @see BinaryStubData
 * @see org.checkerframework.framework.stubifier.BinaryStubWriter
 */
public class BinaryStubReader {

    /** Do not instantiate; all methods are static. */
    private BinaryStubReader() {}

    /**
     * Applies the package and module annotations from the binary stub data eagerly.
     *
     * @param data the complete binary stub data, providing the string pool and annotation pool
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes the container for the parsed annotations
     */
    public static void applyPackageAndModuleRecords(
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        AnnotationFileAnnotations annotationFileAnnos = elementTypes.annotationFileAnnos;

        for (Map.Entry<String, int[]> entry : data.packages.entrySet()) {
            AnnotationMirrorSet pkgDeclAnnos =
                    parseDeclAnnos(entry.getValue(), null, data, atypeFactory, elementTypes);
            if (!pkgDeclAnnos.isEmpty()) {
                annotationFileAnnos.declAnnos.put(entry.getKey(), pkgDeclAnnos);
            }
        }
        for (Map.Entry<String, int[]> entry : data.modules.entrySet()) {
            AnnotationMirrorSet modDeclAnnos =
                    parseDeclAnnos(entry.getValue(), null, data, atypeFactory, elementTypes);
            if (!modDeclAnnos.isEmpty()) {
                annotationFileAnnos.declAnnos.put(entry.getKey(), modDeclAnnos);
            }
        }
    }

    /**
     * Applies the annotations from a {@link BinaryStubData.ClassRecord} to the given class and its
     * members. The annotations are converted to {@link AnnotationMirror}s and stored in the
     * appropriate maps within {@code elementTypes}.
     *
     * @param cr the class record from the binary stub data
     * @param className the fully-qualified name of the class
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes the container for the parsed annotations
     * @param data the complete binary stub data, providing the string pool and annotation pool
     */
    public static void applyClassRecord(
            BinaryStubData.ClassRecord cr,
            String className,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data) {

        AnnotationFileAnnotations annotationFileAnnos = elementTypes.annotationFileAnnos;

        TypeElement typeElt =
                atypeFactory.getProcessingEnv().getElementUtils().getTypeElement(className);
        if (typeElt == null) {
            return;
        }

        // Apply class declaration annotations.
        AnnotationMirrorSet classDeclAnnos =
                parseDeclAnnos(cr.declAnnos, className, data, atypeFactory, elementTypes);
        if (!classDeclAnnos.isEmpty()) {
            annotationFileAnnos.declAnnos.put(
                    ElementUtils.getQualifiedName(typeElt), classDeclAnnos);
        }

        // Process fields.
        Map<String, VariableElement> fieldsByName = new HashMap<>();
        for (VariableElement ve : ElementFilter.fieldsIn(typeElt.getEnclosedElements())) {
            fieldsByName.put(ve.getSimpleName().toString(), ve);
        }

        for (BinaryStubData.FieldRecord fr : cr.fields) {
            String fieldName = data.stringPool[fr.nameIndex];
            VariableElement ve = fieldsByName.get(fieldName);
            if (ve != null) {
                AnnotationMirrorSet fieldDeclAnnos =
                        parseDeclAnnos(fr.declAnnos, className, data, atypeFactory, elementTypes);
                if (!fieldDeclAnnos.isEmpty()) {
                    annotationFileAnnos.declAnnos.putIfAbsent(
                            ElementUtils.getQualifiedName(ve), fieldDeclAnnos);
                }
                if (fr.typeAnnos.length > 0) {
                    // Only apply if not already present from a user-supplied stub file.
                    if (!annotationFileAnnos.atypes.containsKey(ve)) {
                        AnnotatedTypeMirror atm =
                                AnnotatedTypeMirror.createType(ve.asType(), atypeFactory, false);
                        applyTypeAnnos(
                                atm, fr.typeAnnos, className, data, atypeFactory, elementTypes);
                        annotationFileAnnos.atypes.put(ve, atm);
                    }
                }
            }
        }

        Map<String, ExecutableElement> methodsBySig = elementTypes.methodSigIndex(typeElt);
        Map<String, ExecutableElement> ctorsBySig = elementTypes.constructorSigIndex(typeElt);

        for (BinaryStubData.MethodRecord mr : cr.methods) {
            String sig = data.stringPool[mr.sigIndex];
            ExecutableElement ee = methodsBySig.get(sig);
            if (ee == null) {
                ee = ctorsBySig.get(sig);
            }
            if (ee != null) {
                AnnotationMirrorSet methodDeclAnnos =
                        parseDeclAnnos(mr.declAnnos, className, data, atypeFactory, elementTypes);
                if (!methodDeclAnnos.isEmpty()) {
                    annotationFileAnnos.declAnnos.putIfAbsent(
                            ElementUtils.getQualifiedName(ee), methodDeclAnnos);
                }

                boolean hasAnyTypeAnnos =
                        mr.returnTypeAnnos.length > 0 || mr.receiverAnnos.length > 0;
                for (int i = 0; i < mr.paramAnnos.length; i++) {
                    if (mr.paramAnnos[i].length > 0 || mr.paramDeclAnnos[i].length > 0) {
                        hasAnyTypeAnnos = true;
                    }
                }

                // Only apply type annotations if this method is not already present from a
                // user-supplied stub file.
                if (hasAnyTypeAnnos && !annotationFileAnnos.atypes.containsKey(ee)) {
                    AnnotatedExecutableType aet =
                            (AnnotatedExecutableType)
                                    AnnotatedTypeMirror.createType(
                                            ee.asType(), atypeFactory, false);
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

                    for (int i = 0;
                            i < mr.paramAnnos.length && i < aet.getParameterTypes().size();
                            i++) {
                        AnnotatedTypeMirror pType = aet.getParameterTypes().get(i);
                        VariableElement pElt = ee.getParameters().get(i);

                        AnnotationMirrorSet paramDeclAnnos =
                                parseDeclAnnos(
                                        mr.paramDeclAnnos[i],
                                        className,
                                        data,
                                        atypeFactory,
                                        elementTypes);
                        if (!paramDeclAnnos.isEmpty()) {
                            annotationFileAnnos.declAnnos.putIfAbsent(
                                    ElementUtils.getQualifiedName(pElt), paramDeclAnnos);
                            if (!annotationFileAnnos.atypes.containsKey(pElt)) {
                                AnnotatedTypeMirror pInner = getInnermostComponentType(pType);
                                for (AnnotationMirror am : paramDeclAnnos) {
                                    pInner.replaceAnnotation(am);
                                }
                            }
                        }

                        applyTypeAnnos(
                                pType,
                                mr.paramAnnos[i],
                                className,
                                data,
                                atypeFactory,
                                elementTypes);
                        annotationFileAnnos.atypes.putIfAbsent(pElt, pType);
                    }
                    annotationFileAnnos.atypes.put(ee, aet);
                }
            }
        }
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
            if (idx >= 0 && idx < data.annotationPool.length) {
                AnnotationMirror am =
                        getAnnotationMirror(
                                data.annotationPool[idx],
                                enclosingClassName,
                                atypeFactory,
                                data,
                                elementTypes);
                if (am != null) {
                    set.add(am);
                }
            }
        }
        return set;
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
        while (current != null && current.getKind() == TypeKind.ARRAY) {
            current = ((AnnotatedArrayType) current).getComponentType();
        }
        return current;
    }

    /**
     * Applies a list of type annotations to {@code atm}, navigating each annotation's type path to
     * locate the annotated component. Also adds {@link
     * org.checkerframework.framework.qual.FromStubFile} to each annotated component.
     *
     * @param atm the annotated type mirror to annotate
     * @param annos the type annotations to apply
     * @param enclosingClassName fully-qualified name of the enclosing class, if any
     * @param data the binary stub data
     * @param atypeFactory the type factory of the currently-running checker
     * @param elementTypes the container for the parsed annotations, including a cached
     *     {@code @FromStubFile} mirror
     */
    private static void applyTypeAnnos(
            AnnotatedTypeMirror atm,
            BinaryStubData.TypeAnno[] annos,
            String enclosingClassName,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        for (BinaryStubData.TypeAnno ta : annos) {
            if (ta.annoIndex >= 0 && ta.annoIndex < data.annotationPool.length) {
                AnnotationMirror am =
                        getAnnotationMirror(
                                data.annotationPool[ta.annoIndex],
                                enclosingClassName,
                                atypeFactory,
                                data,
                                elementTypes);
                if (am != null) {
                    AnnotatedTypeMirror target = resolvePath(atm, ta.path);
                    if (target != null) {
                        target.replaceAnnotation(am);
                        target.addAnnotation(elementTypes.fromStubFileAnno);
                    }
                }
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
            if (current == null) return null;
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
                    if (step.argIndex < adt.getTypeArguments().size()) {
                        current = adt.getTypeArguments().get(step.argIndex);
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
     * AnnotationMirror}. Results are memoised in the per-factory cache held by {@code
     * elementTypes}. Annotations whose type is not on the annotation-processor classpath are
     * silently skipped.
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
        if (hasNameLiteralValue(ar)) {
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
        if (val instanceof Boolean) builder.setValue(name, (Boolean) val);
        else if (val instanceof Character) builder.setValue(name, (Character) val);
        else if (val instanceof Double) builder.setValue(name, (Double) val);
        else if (val instanceof Float) builder.setValue(name, (Float) val);
        else if (val instanceof Integer) builder.setValue(name, (Integer) val);
        else if (val instanceof Long) builder.setValue(name, (Long) val);
        else if (val instanceof Short) builder.setValue(name, (Short) val);
        else if (val instanceof Byte) builder.setValue(name, (Short) ((Byte) val).shortValue());
        else if (val instanceof String) builder.setValue(name, (String) val);
        else if (val instanceof TypeMirror) builder.setValue(name, (TypeMirror) val);
        else if (val instanceof VariableElement) builder.setValue(name, (VariableElement) val);
        else if (val instanceof AnnotationMirror) builder.setValue(name, (AnnotationMirror) val);
    }

    /**
     * Helper to recursively look up a field name in a TypeElement and its hierarchy.
     *
     * @param te the type element to search
     * @param name the simple name of the field to find
     * @param env the processing environment
     * @return the resolved variable element, or {@code null} if not found
     */
    private static VariableElement findFieldInType(
            TypeElement te, String name, ProcessingEnvironment env) {
        if (te == null) return null;
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
                if (ve != null) return ve;
            }
        }
        for (TypeMirror itf : te.getInterfaces()) {
            if (itf.getKind() == TypeKind.DECLARED) {
                Element itfElt = env.getTypeUtils().asElement(itf);
                if (itfElt instanceof TypeElement) {
                    VariableElement ve = findFieldInType((TypeElement) itfElt, name, env);
                    if (ve != null) return ve;
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
     * @param expectedKind the expected type kind of the element
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
            Long l = (Long) val;
            if (expectedKind == TypeKind.LONG) return l;
            if (expectedKind == TypeKind.SHORT) return l.shortValue();
            if (expectedKind == TypeKind.BYTE) return l.byteValue();
            if (expectedKind == TypeKind.CHAR) return (char) l.longValue();
            return l.intValue();
        } else if (val instanceof Double) {
            Double d = (Double) val;
            if (expectedKind == TypeKind.FLOAT) return d.floatValue();
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
                TypeElement te = env.getElementUtils().getTypeElement(fqName);
                if (te != null) {
                    TypeMirror type = te.asType();
                    cache.resolvedClassTypesCache.put(fqName, type);
                    return type;
                }
            } else {
                TypeElement te = env.getElementUtils().getTypeElement(fqName);
                if (te != null) {
                    return te.asType();
                }
            }
        } else if (val instanceof BinaryStubData.EnumConstantValue) {
            BinaryStubData.EnumConstantValue ev = (BinaryStubData.EnumConstantValue) val;
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
            throw new RuntimeException(
                    "Could not resolve NameLiteralValue constant: "
                            + constantName
                            + " in enclosing class: "
                            + enclosingClassName);
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
     * @param elementTypes the container for the parsed annotations
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
        TypeMirror returnType = null;
        try {
            ExecutableElement elem = builder.findElement(name);
            returnType = elem.getReturnType();
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
            if (expectedKind == TypeKind.LONG) return (long) charCode;
            if (expectedKind == TypeKind.SHORT) return (short) charCode;
            if (expectedKind == TypeKind.BYTE) return (byte) charCode;
            if (expectedKind == TypeKind.CHAR) return (char) charCode;
            return charCode;
        } else if (cVal instanceof Number) {
            Number n = (Number) cVal;
            if (expectedKind == TypeKind.LONG) return n.longValue();
            if (expectedKind == TypeKind.SHORT) return n.shortValue();
            if (expectedKind == TypeKind.BYTE) return n.byteValue();
            if (expectedKind == TypeKind.CHAR) return (char) n.longValue();
            return n.intValue();
        }
        return cVal;
    }

    /**
     * Helper to create an AnnotationMirror structurally without caching the outer level.
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
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Recursively checks if an annotation record or value contains any name literal constant
     * references.
     *
     * @param val the value to inspect
     * @return {@code true} if a NameLiteralValue is found, {@code false} otherwise
     */
    private static boolean hasNameLiteralValue(Object val) {
        if (val instanceof BinaryStubData.NameLiteralValue) {
            return true;
        } else if (val instanceof List) {
            for (Object item : (List<?>) val) {
                if (hasNameLiteralValue(item)) {
                    return true;
                }
            }
        } else if (val instanceof BinaryStubData.AnnotationRecord) {
            BinaryStubData.AnnotationRecord ar = (BinaryStubData.AnnotationRecord) val;
            for (Object v : ar.elementValues.values()) {
                if (hasNameLiteralValue(v)) {
                    return true;
                }
            }
        }
        return false;
    }
}
