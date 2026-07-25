package org.checkerframework.framework.util.element;

import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.TargetType;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.element.ElementAnnotationUtil.UnexpectedAnnotationLocationException;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Applies Element annotations to a single AnnotatedTypeVariable representing a type parameter.
 * Note, the index of IndexedElementAnnotationApplier refers to the type parameter's index in the
 * list that encloses it.
 */
abstract class TypeParamElementAnnotationApplier extends IndexedElementAnnotationApplier {

    /**
     * Returns true if element is a TYPE_PARAMETER.
     *
     * @param typeMirror ignored
     * @param element the element that might be a TYPE_PARAMETER
     * @return true if element is a TYPE_PARAMETER
     */
    public static boolean accepts(AnnotatedTypeMirror typeMirror, Element element) {
        return element.getKind() == ElementKind.TYPE_PARAMETER;
    }

    protected final AnnotatedTypeVariable typeParam;
    protected final AnnotatedTypeFactory atypeFactory;

    /**
     * Returns target type that represents the location of the lower bound of element.
     *
     * @return target type that represents the location of the lower bound of element
     */
    protected abstract TargetType lowerBoundTarget();

    /**
     * Returns target type that represents the location of the upper bound of element.
     *
     * @return target type that represents the location of the upper bound of element
     */
    protected abstract TargetType upperBoundTarget();

    /**
     * Constructor.
     *
     * @param type the type to annotate
     * @param element the corresponding element
     * @param atypeFactory the type factory
     */
    /*package-private*/ TypeParamElementAnnotationApplier(
            AnnotatedTypeVariable type, Element element, AnnotatedTypeFactory atypeFactory) {
        super(type, element);
        this.typeParam = type;
        this.atypeFactory = atypeFactory;
    }

    /**
     * Cached {lower-bound, upper-bound} target pair. Lazily initialized in {@link
     * #annotatedTargets()}; populated at most once per applier instance.
     */
    private TargetType @MonotonicNonNull [] cachedAnnotatedTargets;

    /**
     * Returns the lower bound and upper bound targets.
     *
     * @return the lower bound and upper bound targets
     */
    @Override
    protected TargetType[] annotatedTargets() {
        TargetType[] result = cachedAnnotatedTargets;
        if (result == null) {
            result = new TargetType[] {lowerBoundTarget(), upperBoundTarget()};
            cachedAnnotatedTargets = result;
        }
        return result;
    }

    /**
     * Returns the parameter_index of anno's TypeAnnotationPosition which will actually point to the
     * type parameter's index in its enclosing type parameter list.
     *
     * @return the parameter_index of anno's TypeAnnotationPosition which will actually point to the
     *     type parameter's index in its enclosing type parameter list
     */
    @Override
    public int getTypeCompoundIndex(TypeCompound anno) {
        return anno.getPosition().parameter_index;
    }

    /**
     * @param targeted the list of annotations that were on the lower/upper bounds of the type
     *     parameter
     *     <p>Note: When handling type parameters we NEVER add primary annotations to the type
     *     parameter. Primary annotations are reserved for the use of a type parameter
     *     (e.g. @Nullable T t; )
     *     <p>If an annotation is present on the type parameter itself, it represents the
     *     lower-bound annotation of that type parameter. Any annotation on the extends bound of a
     *     type parameter is placed on that bound.
     */
    @Override
    protected void handleTargeted(List<TypeCompound> targeted)
            throws UnexpectedAnnotationLocationException {
        int paramIndex = getElementIndex();
        List<TypeCompound> upperBoundAnnos = new ArrayList<>();
        List<TypeCompound> lowerBoundAnnos = new ArrayList<>();

        for (TypeCompound anno : targeted) {
            AnnotationMirror aliasedAnno = atypeFactory.canonicalAnnotation(anno);
            AnnotationMirror canonicalAnno = (aliasedAnno != null) ? aliasedAnno : anno;

            if (anno.position.parameter_index != paramIndex
                    || !atypeFactory.isSupportedQualifier(canonicalAnno)) {
                continue;
            }

            if (ElementAnnotationUtil.isOnComponentType(anno)) {
                applyComponentAnnotation(anno);
            } else if (anno.position.type == upperBoundTarget()) {
                upperBoundAnnos.add(anno);
            } else {
                lowerBoundAnnos.add(anno);
            }
        }

        applyLowerBounds(lowerBoundAnnos);
        applyUpperBounds(upperBoundAnnos);

        // A primary annotation on a type parameter that has no explicit `extends` clause (e.g.
        // `<@NonNull T>`) is equivalent to annotating the implicit `Object` upper bound (e.g.
        // `<@NonNull T extends @NonNull Object>`); see the "Examples of qualifiers on a type
        // parameter" section of the manual.  The primary annotation is applied to the lower bound
        // above; also copy it onto the implicit upper bound so that the two forms behave
        // identically.  Hierarchies without a primary annotation are left for the defaulting
        // mechanism to fill in.  This is only done for unbounded type parameters: an explicit
        // bound (even the unannotated `extends Object`) is governed by the explicit-upper-bound
        // defaults instead.
        if (upperBoundAnnos.isEmpty() && !lowerBoundAnnos.isEmpty() && isUnboundedTypeVariable()) {
            typeParam.getUpperBound().addAnnotations(lowerBoundAnnos);
        }
    }

    /**
     * Returns true if this type parameter has no explicit {@code extends} clause, as in {@code
     * <@NonNull T>}. Mirrors {@code QualifierDefaults.getTypeVarBoundType}.
     *
     * @return true if this type parameter has no explicit {@code extends} clause
     */
    private boolean isUnboundedTypeVariable() {
        TreePath path = atypeFactory.getTreeUtils().getPath(element);
        Tree leaf = path == null ? null : path.getLeaf();
        if (leaf instanceof TypeParameterTree) {
            List<? extends Tree> bounds = ((TypeParameterTree) leaf).getBounds();
            return bounds == null || bounds.isEmpty();
        }
        // No source tree is available (e.g. the type parameter comes from a class file). If the
        // sole bound is Object, assume it was not written explicitly.
        if (element instanceof TypeParameterElement) {
            List<? extends TypeMirror> bounds = ((TypeParameterElement) element).getBounds();
            return bounds.size() == 1 && TypesUtils.isObject(bounds.get(0));
        }
        return false;
    }

    /**
     * Applies a list of annotations to the upperBound of the type parameter. If the type of the
     * upper bound is an intersection we must first find the correct location for each annotation.
     */
    private void applyUpperBounds(List<TypeCompound> upperBounds) {
        if (!upperBounds.isEmpty()) {
            AnnotatedTypeMirror upperBoundType = typeParam.getUpperBound();

            if (upperBoundType.getKind() == TypeKind.INTERSECTION) {
                List<AnnotatedTypeMirror> bounds =
                        ((AnnotatedIntersectionType) upperBoundType).getBounds();
                int boundIndexOffset = ElementAnnotationUtil.getBoundIndexOffset(bounds);

                for (TypeCompound anno : upperBounds) {
                    int boundIndex = anno.position.bound_index + boundIndexOffset;

                    if (boundIndex < 0 || boundIndex >= bounds.size()) {
                        throw new BugInCF(
                                "Invalid bound index on element annotation ( "
                                        + anno
                                        + " ) "
                                        + "for type ( "
                                        + typeParam
                                        + " ) with "
                                        + "upper bound ( "
                                        + typeParam.getUpperBound()
                                        + " ) "
                                        + "and boundIndex( "
                                        + boundIndex
                                        + " ) ");
                    }

                    bounds.get(boundIndex).replaceAnnotation(anno); // TODO: WHY NOT ADD?
                }
                ((AnnotatedIntersectionType) upperBoundType).copyIntersectionBoundAnnotations();

            } else {
                upperBoundType.addAnnotations(upperBounds);
            }
        }
    }

    /**
     * In the event of multiple annotations on an AnnotatedNullType lower bound we want to preserve
     * the multiple annotations so that a type.invalid error is issued later.
     *
     * @param annos the annotations to add to the lower bound
     */
    private void applyLowerBounds(List<? extends AnnotationMirror> annos) {
        if (!annos.isEmpty()) {
            AnnotatedTypeMirror lowerBound = typeParam.getLowerBound();

            for (AnnotationMirror anno : annos) {
                lowerBound.addAnnotation(anno);
            }
        }
    }

    /**
     * Apply the component annotation.
     *
     * @param anno the compound type
     * @throws UnexpectedAnnotationLocationException when an unexpected annotation location is
     *     encountered
     */
    private void applyComponentAnnotation(TypeCompound anno)
            throws UnexpectedAnnotationLocationException {
        AnnotatedTypeMirror upperBoundType = typeParam.getUpperBound();

        // Determine the target type, then dispatch on it.
        AnnotatedTypeMirror targetType;
        if (anno.position.type == upperBoundTarget()) {
            if (upperBoundType.getKind() == TypeKind.INTERSECTION) {
                List<AnnotatedTypeMirror> bounds =
                        ((AnnotatedIntersectionType) upperBoundType).getBounds();
                int boundIndex =
                        anno.position.bound_index
                                + ElementAnnotationUtil.getBoundIndexOffset(bounds);

                if (boundIndex < 0 || boundIndex >= bounds.size()) {
                    throw new BugInCF(
                            "Invalid bound index on element annotation ( "
                                    + anno
                                    + " ) "
                                    + "for type ( "
                                    + typeParam
                                    + " ) with upper bound ( "
                                    + typeParam.getUpperBound()
                                    + " )");
                }
                targetType = bounds.get(boundIndex);
            } else {
                targetType = upperBoundType;
            }
        } else {
            targetType = typeParam.getLowerBound();
        }

        ElementAnnotationUtil.annotateViaTypeAnnoPosition(
                targetType, Collections.singletonList(anno));
    }
}
