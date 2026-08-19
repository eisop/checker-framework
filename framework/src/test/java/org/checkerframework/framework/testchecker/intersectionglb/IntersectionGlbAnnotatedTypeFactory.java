package org.checkerframework.framework.testchecker.intersectionglb;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbA;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbB;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbC;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbD;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbE;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbF;
import org.checkerframework.framework.testchecker.lubglb.quals.PolyLubglb;
import org.checkerframework.framework.type.QualifierHierarchy;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

/** The type factory for {@link IntersectionGlbChecker}. */
public class IntersectionGlbAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates an IntersectionGlbAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public IntersectionGlbAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new HashSet<Class<? extends Annotation>>(
                Arrays.asList(
                        LubglbA.class,
                        LubglbB.class,
                        LubglbC.class,
                        LubglbD.class,
                        LubglbE.class,
                        LubglbF.class,
                        PolyLubglb.class));
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation is order-independent: it returns the greatest lower bound of the two
     * qualifiers.
     */
    @Override
    protected AnnotationMirror combineIntersectionBoundAnnotationsInHierarchy(
            AnnotationMirror existingAnnotation,
            AnnotationMirror newAnnotation,
            QualifierHierarchy qualifierHierarchy) {
        return qualifierHierarchy.greatestLowerBoundQualifiersOnly(
                existingAnnotation, newAnnotation);
    }
}
