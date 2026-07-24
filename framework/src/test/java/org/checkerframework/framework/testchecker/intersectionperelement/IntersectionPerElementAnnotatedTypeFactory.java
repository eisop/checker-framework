package org.checkerframework.framework.testchecker.intersectionperelement;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.A1;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.ATop;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.B1;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.BTop;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Type factory for {@link IntersectionPerElementChecker}. It defines two independent qualifier
 * hierarchies (A and B) and opts into per-element intersection-bound semantics by overriding {@link
 * #shouldHomogenizeIntersectionBounds()} to return false.
 */
public class IntersectionPerElementAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Creates a new IntersectionPerElementAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    @SuppressWarnings("this-escape")
    public IntersectionPerElementAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(ATop.class, A1.class, BTop.class, B1.class);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns false: this checker keeps each intersection bound's own qualifier instead of
     * homogenizing the bounds to the intersection's primary annotation.
     */
    @Override
    protected boolean shouldHomogenizeIntersectionBounds() {
        return false;
    }
}
