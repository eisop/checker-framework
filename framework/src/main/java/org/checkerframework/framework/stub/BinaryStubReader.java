package org.checkerframework.framework.stub;

import org.checkerframework.framework.stub.AnnotationFileParser.AnnotationFileAnnotations;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.ElementUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

/**
 * Applies annotation data from a {@link BinaryStubData.ClassRecord} to a checker's {@link
 * AnnotationFileAnnotations}. This is the read-side counterpart of {@code BinaryStubWriter}: it
 * translates the compact binary representation back into {@link AnnotationMirror} and {@link
 * AnnotatedTypeMirror} objects that the framework uses during type-checking.
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

    /**
     * Applies the package and module annotations from the binary stub data eagerly.
     *
     * @param data the complete binary stub data, providing the string pool
     * @param atypeFactory the factory used to create types and parse annotations
     * @param elementTypes the container for the parsed annotations
     */
    public static void applyPackageAndModuleRecords(
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes) {
        AnnotationFileAnnotations annotationFileAnnos = elementTypes.annotationFileAnnos;
        Map<String, AnnotationMirror> annoCache = elementTypes.binaryAnnoCache;

        for (Map.Entry<String, int[]> entry : data.packages.entrySet()) {
            AnnotationMirrorSet pkgDeclAnnos =
                    parseDeclAnnos(entry.getValue(), data, atypeFactory, annoCache);
            if (!pkgDeclAnnos.isEmpty()) {
                annotationFileAnnos.declAnnos.put(entry.getKey(), pkgDeclAnnos);
            }
        }
        for (Map.Entry<String, int[]> entry : data.modules.entrySet()) {
            AnnotationMirrorSet modDeclAnnos =
                    parseDeclAnnos(entry.getValue(), data, atypeFactory, annoCache);
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
     * @param data the complete binary stub data, providing the string pool
     */
    public static void applyClassRecord(
            BinaryStubData.ClassRecord cr,
            String className,
            AnnotatedTypeFactory atypeFactory,
            AnnotationFileElementTypes elementTypes,
            BinaryStubData data) {

        AnnotationFileAnnotations annotationFileAnnos = elementTypes.annotationFileAnnos;
        Map<String, AnnotationMirror> annoCache = elementTypes.binaryAnnoCache;

        TypeElement typeElt =
                atypeFactory.getProcessingEnv().getElementUtils().getTypeElement(className);
        if (typeElt == null) {
            return;
        }

        // Apply class declaration annotations.
        AnnotationMirrorSet classDeclAnnos =
                parseDeclAnnos(cr.declAnnos, data, atypeFactory, annoCache);
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
                        parseDeclAnnos(fr.declAnnos, data, atypeFactory, annoCache);
                if (!fieldDeclAnnos.isEmpty()) {
                    annotationFileAnnos.declAnnos.putIfAbsent(
                            ElementUtils.getQualifiedName(ve), fieldDeclAnnos);
                }
                if (fr.typeAnnos.length > 0 || !fieldDeclAnnos.isEmpty()) {
                    // Only apply if not already present from a user-supplied stub file.
                    if (!annotationFileAnnos.atypes.containsKey(ve)) {
                        AnnotatedTypeMirror atm =
                                AnnotatedTypeMirror.createType(ve.asType(), atypeFactory, false);
                        AnnotatedTypeMirror inner = getInnermostComponentType(atm);
                        for (AnnotationMirror am : fieldDeclAnnos) {
                            inner.addAnnotation(am);
                        }
                        applyTypeAnnos(atm, fr.typeAnnos, data, atypeFactory, annoCache);
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
                        parseDeclAnnos(mr.declAnnos, data, atypeFactory, annoCache);
                if (!methodDeclAnnos.isEmpty()) {
                    annotationFileAnnos.declAnnos.putIfAbsent(
                            ElementUtils.getQualifiedName(ee), methodDeclAnnos);
                }

                boolean hasAnyTypeAnnos =
                        mr.returnTypeAnnos.length > 0
                                || mr.receiverAnnos.length > 0
                                || !methodDeclAnnos.isEmpty();
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

                    AnnotatedTypeMirror retInner = getInnermostComponentType(aet.getReturnType());
                    for (AnnotationMirror am : methodDeclAnnos) {
                        retInner.addAnnotation(am);
                    }
                    applyTypeAnnos(
                            aet.getReturnType(), mr.returnTypeAnnos, data, atypeFactory, annoCache);

                    if (aet.getReceiverType() != null && mr.receiverAnnos.length > 0) {
                        applyTypeAnnos(
                                aet.getReceiverType(),
                                mr.receiverAnnos,
                                data,
                                atypeFactory,
                                annoCache);
                    }

                    for (int i = 0;
                            i < mr.paramAnnos.length && i < aet.getParameterTypes().size();
                            i++) {
                        AnnotatedTypeMirror pType = aet.getParameterTypes().get(i);
                        VariableElement pElt = ee.getParameters().get(i);

                        AnnotationMirrorSet paramDeclAnnos =
                                parseDeclAnnos(mr.paramDeclAnnos[i], data, atypeFactory, annoCache);
                        if (!paramDeclAnnos.isEmpty()) {
                            annotationFileAnnos.declAnnos.putIfAbsent(
                                    ElementUtils.getQualifiedName(pElt), paramDeclAnnos);
                            if (!annotationFileAnnos.atypes.containsKey(pElt)) {
                                AnnotatedTypeMirror pInner = getInnermostComponentType(pType);
                                for (AnnotationMirror am : paramDeclAnnos) {
                                    pInner.addAnnotation(am);
                                }
                            }
                        }

                        applyTypeAnnos(pType, mr.paramAnnos[i], data, atypeFactory, annoCache);
                        annotationFileAnnos.atypes.putIfAbsent(pElt, pType);
                    }
                    annotationFileAnnos.atypes.put(ee, aet);
                }
            }
        }
    }

    /**
     * Parses a list of string-pool indices into a set of {@link AnnotationMirror}s. Indices that
     * cannot be resolved are silently skipped.
     *
     * @param annoIndices string-pool indices of the serialised annotation texts
     * @param data the binary stub data providing the string pool
     * @param atypeFactory the type factory of the currently-running checker
     * @param annoCache a per-class cache from annotation text to mirror, to avoid re-parsing
     * @return the set of successfully resolved annotation mirrors
     */
    private static AnnotationMirrorSet parseDeclAnnos(
            int[] annoIndices,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            Map<String, AnnotationMirror> annoCache) {
        AnnotationMirrorSet set = new AnnotationMirrorSet();
        for (int idx : annoIndices) {
            AnnotationMirror am = parseAnnotation(data.stringPool[idx], atypeFactory, annoCache);
            if (am != null) {
                set.add(am);
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
     * @param data the binary stub data providing the string pool
     * @param atypeFactory the type factory of the currently-running checker
     * @param annoCache a per-class cache from annotation text to mirror
     */
    private static void applyTypeAnnos(
            AnnotatedTypeMirror atm,
            BinaryStubData.TypeAnno[] annos,
            BinaryStubData data,
            AnnotatedTypeFactory atypeFactory,
            Map<String, AnnotationMirror> annoCache) {
        for (BinaryStubData.TypeAnno ta : annos) {
            AnnotationMirror am =
                    parseAnnotation(data.stringPool[ta.annoIndex], atypeFactory, annoCache);
            if (am != null) {
                AnnotatedTypeMirror target = resolvePath(atm, ta.path);
                if (target != null) {
                    target.addAnnotation(am);
                    AnnotationMirror fsfa =
                            org.checkerframework.javacutil.AnnotationBuilder.fromClass(
                                    atypeFactory.getElementUtils(),
                                    org.checkerframework.framework.qual.FromStubFile.class);
                    target.addAnnotation(fsfa);
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
     * Parses a serialised annotation string (as produced by {@code BinaryStubWriter}) into an
     * {@link AnnotationMirror}. Results are memoised in {@code annoCache}. Annotations whose type
     * is not on the annotation-processor classpath (e.g. JDK-internal {@code @DefinedBy}) are
     * silently skipped and cached as {@code null}.
     *
     * @param annoStr the serialised annotation text, e.g. {@code
     *     "@org.checkerframework.checker.nullness.qual.Nullable"}
     * @param atypeFactory the type factory of the currently-running checker
     * @param annoCache a per-class cache from annotation text to mirror; {@code null} entries
     *     indicate that the annotation could not be resolved and should be skipped
     * @return the parsed annotation mirror, or {@code null} if the annotation cannot be resolved
     */
    private static AnnotationMirror parseAnnotation(
            String annoStr,
            AnnotatedTypeFactory atypeFactory,
            Map<String, AnnotationMirror> annoCache) {
        return annoCache.computeIfAbsent(
                annoStr,
                s -> {
                    try {
                        // Fast path for marker annotations to avoid JavaParser overhead
                        if (!s.contains("(")) {
                            String fqn = s.substring(1).trim(); // remove '@'
                            return org.checkerframework.javacutil.AnnotationBuilder.fromName(
                                    atypeFactory.getProcessingEnv().getElementUtils(), fqn);
                        }

                        com.github.javaparser.ast.expr.AnnotationExpr ae =
                                com.github.javaparser.StaticJavaParser.parseAnnotation(s);
                        String fqn = ae.getNameAsString();
                        if (ae instanceof com.github.javaparser.ast.expr.MarkerAnnotationExpr) {
                            return org.checkerframework.javacutil.AnnotationBuilder.fromName(
                                    atypeFactory.getProcessingEnv().getElementUtils(), fqn);
                        }
                        org.checkerframework.javacutil.AnnotationBuilder builder =
                                new org.checkerframework.javacutil.AnnotationBuilder(
                                        atypeFactory.getProcessingEnv(), fqn);
                        if (ae
                                instanceof
                                com.github.javaparser.ast.expr.SingleMemberAnnotationExpr) {
                            com.github.javaparser.ast.expr.Expression valExpr =
                                    ((com.github.javaparser.ast.expr.SingleMemberAnnotationExpr) ae)
                                            .getMemberValue();
                            addValueToBuilder(
                                    builder, "value", valExpr, atypeFactory.getProcessingEnv());
                        } else if (ae
                                instanceof com.github.javaparser.ast.expr.NormalAnnotationExpr) {
                            for (com.github.javaparser.ast.expr.MemberValuePair mvp :
                                    ((com.github.javaparser.ast.expr.NormalAnnotationExpr) ae)
                                            .getPairs()) {
                                addValueToBuilder(
                                        builder,
                                        mvp.getNameAsString(),
                                        mvp.getValue(),
                                        atypeFactory.getProcessingEnv());
                            }
                        }
                        return builder.build();
                    } catch (Throwable e) {
                        // Silently ignore annotations that cannot be parsed or resolved
                        // (e.g. JDK-internal annotations like @DefinedBy that are not on the
                        // classpath).
                        return null;
                    }
                });
    }

    /**
     * Parses an annotation member value expression into an object suitable for passing to {@link
     * org.checkerframework.javacutil.AnnotationBuilder#setValue}. Returns {@code null} if the
     * expression kind is not supported.
     *
     * @param expr the JavaParser expression representing the annotation value
     * @param expectedKind the expected type kind of the annotation member (used to select among
     *     integer widths)
     * @param env the annotation processing environment
     * @return the parsed value, or {@code null} if unsupported
     */
    private static Object parseAnnotationValue(
            com.github.javaparser.ast.expr.Expression expr,
            javax.lang.model.type.TypeKind expectedKind,
            ProcessingEnvironment env) {
        if (expr instanceof com.github.javaparser.ast.expr.StringLiteralExpr) {
            return ((com.github.javaparser.ast.expr.StringLiteralExpr) expr).getValue();
        } else if (expr instanceof com.github.javaparser.ast.expr.BooleanLiteralExpr) {
            return ((com.github.javaparser.ast.expr.BooleanLiteralExpr) expr).getValue();
        } else if (expr instanceof com.github.javaparser.ast.expr.IntegerLiteralExpr) {
            Number num = ((com.github.javaparser.ast.expr.IntegerLiteralExpr) expr).asNumber();
            if (expectedKind == javax.lang.model.type.TypeKind.LONG) return num.longValue();
            if (expectedKind == javax.lang.model.type.TypeKind.SHORT) return num.shortValue();
            if (expectedKind == javax.lang.model.type.TypeKind.BYTE) return num.byteValue();
            return num.intValue();
        } else if (expr instanceof com.github.javaparser.ast.expr.UnaryExpr) {
            com.github.javaparser.ast.expr.UnaryExpr ue =
                    (com.github.javaparser.ast.expr.UnaryExpr) expr;
            if (ue.getOperator() == com.github.javaparser.ast.expr.UnaryExpr.Operator.MINUS
                    && ue.getExpression()
                            instanceof com.github.javaparser.ast.expr.IntegerLiteralExpr) {
                Number num =
                        ((com.github.javaparser.ast.expr.IntegerLiteralExpr) ue.getExpression())
                                .asNumber();
                if (expectedKind == javax.lang.model.type.TypeKind.LONG) return -num.longValue();
                if (expectedKind == javax.lang.model.type.TypeKind.SHORT)
                    return (short) -num.shortValue();
                if (expectedKind == javax.lang.model.type.TypeKind.BYTE)
                    return (byte) -num.byteValue();
                return -num.intValue();
            } else if (ue.getOperator() == com.github.javaparser.ast.expr.UnaryExpr.Operator.MINUS
                    && ue.getExpression()
                            instanceof com.github.javaparser.ast.expr.LongLiteralExpr) {
                return -((com.github.javaparser.ast.expr.LongLiteralExpr) ue.getExpression())
                        .asNumber()
                        .longValue();
            }
        } else if (expr instanceof com.github.javaparser.ast.expr.LongLiteralExpr) {
            return ((com.github.javaparser.ast.expr.LongLiteralExpr) expr).asNumber().longValue();
        } else if (expr instanceof com.github.javaparser.ast.expr.DoubleLiteralExpr) {
            return ((com.github.javaparser.ast.expr.DoubleLiteralExpr) expr).asDouble();
        } else if (expr instanceof com.github.javaparser.ast.expr.CharLiteralExpr) {
            char val = ((com.github.javaparser.ast.expr.CharLiteralExpr) expr).asChar();
            if (expectedKind == javax.lang.model.type.TypeKind.LONG) return (long) val;
            if (expectedKind == javax.lang.model.type.TypeKind.INT) return (int) val;
            if (expectedKind == javax.lang.model.type.TypeKind.SHORT) return (short) val;
            if (expectedKind == javax.lang.model.type.TypeKind.BYTE) return (byte) val;
            return val;
        } else if (expr instanceof com.github.javaparser.ast.expr.ClassExpr) {
            com.github.javaparser.ast.type.Type type =
                    ((com.github.javaparser.ast.expr.ClassExpr) expr).getType();
            TypeElement te = env.getElementUtils().getTypeElement(type.asString());
            if (te != null) return te.asType();
        } else if (expr instanceof com.github.javaparser.ast.expr.FieldAccessExpr) {
            com.github.javaparser.ast.expr.FieldAccessExpr fae =
                    (com.github.javaparser.ast.expr.FieldAccessExpr) expr;
            String enumClassName = fae.getScope().toString();
            String enumConstName = fae.getNameAsString();
            TypeElement enumClass = env.getElementUtils().getTypeElement(enumClassName);
            if (enumClass != null) {
                for (javax.lang.model.element.Element elt : enumClass.getEnclosedElements()) {
                    if (elt.getKind() == javax.lang.model.element.ElementKind.ENUM_CONSTANT
                            && elt.getSimpleName().contentEquals(enumConstName)) {
                        return elt;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Dispatches a scalar annotation-member value to the appropriate {@link
     * org.checkerframework.javacutil.AnnotationBuilder#setValue} overload.
     *
     * @param builder the builder to set the value on
     * @param name the annotation member name
     * @param val the value to set; must be one of the types accepted by {@code
     *     AnnotationBuilder.setValue}
     */
    private static void dispatchSetValue(
            org.checkerframework.javacutil.AnnotationBuilder builder, String name, Object val) {
        if (val instanceof Boolean) builder.setValue(name, (Boolean) val);
        else if (val instanceof Character) builder.setValue(name, (Character) val);
        else if (val instanceof Double) builder.setValue(name, (Double) val);
        else if (val instanceof Float) builder.setValue(name, (Float) val);
        else if (val instanceof Integer) builder.setValue(name, (Integer) val);
        else if (val instanceof Long) builder.setValue(name, (Long) val);
        else if (val instanceof Short) builder.setValue(name, (Short) val);
        else if (val instanceof Byte) builder.setValue(name, (Short) ((Byte) val).shortValue());
        else if (val instanceof String) builder.setValue(name, (String) val);
        else if (val instanceof javax.lang.model.type.TypeMirror)
            builder.setValue(name, (javax.lang.model.type.TypeMirror) val);
        else if (val instanceof javax.lang.model.element.VariableElement)
            builder.setValue(name, (javax.lang.model.element.VariableElement) val);
        else if (val instanceof javax.lang.model.element.AnnotationMirror)
            builder.setValue(name, (javax.lang.model.element.AnnotationMirror) val);
    }

    /**
     * Parses a JavaParser expression representing one annotation member value and sets it on {@code
     * builder}. Array values are wrapped in a singleton or multi-element list as required.
     *
     * @param builder the builder on which to set the value
     * @param name the annotation member name
     * @param expr the JavaParser expression for the value
     * @param env the annotation processing environment
     */
    private static void addValueToBuilder(
            org.checkerframework.javacutil.AnnotationBuilder builder,
            String name,
            com.github.javaparser.ast.expr.Expression expr,
            ProcessingEnvironment env) {
        boolean isArray = false;
        javax.lang.model.type.TypeKind expectedKind = javax.lang.model.type.TypeKind.NONE;
        try {
            javax.lang.model.element.ExecutableElement elem = builder.findElement(name);
            javax.lang.model.type.TypeMirror returnType = elem.getReturnType();
            isArray = returnType.getKind() == javax.lang.model.type.TypeKind.ARRAY;
            if (isArray) {
                expectedKind =
                        ((javax.lang.model.type.ArrayType) returnType).getComponentType().getKind();
            } else {
                expectedKind = returnType.getKind();
            }
        } catch (Exception e) {
            // Silently ignore if the annotation element cannot be found.
        }

        if (expr instanceof com.github.javaparser.ast.expr.ArrayInitializerExpr) {
            List<Object> values = new java.util.ArrayList<>();
            for (com.github.javaparser.ast.expr.Expression e :
                    ((com.github.javaparser.ast.expr.ArrayInitializerExpr) expr).getValues()) {
                Object val = parseAnnotationValue(e, expectedKind, env);
                if (val != null) values.add(val);
            }
            builder.setValue(name, values);
        } else {
            Object val = parseAnnotationValue(expr, expectedKind, env);
            if (val != null) {
                if (isArray) builder.setValue(name, Collections.singletonList(val));
                else dispatchSetValue(builder, name, val);
            }
        }
    }
}
