package org.checkerframework.framework.testchecker.intersectionglb;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker over the {@code lubglb} qualifier hierarchy whose type factory summarizes an
 * intersection type's bounds by their greatest lower bound instead of by first-bound-wins. It tests
 * {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#combineIntersectionBoundAnnotationsInHierarchy},
 * which no checker in this repository overrides.
 */
public class IntersectionGlbChecker extends BaseTypeChecker {

    /** Creates an IntersectionGlbChecker. */
    public IntersectionGlbChecker() {}
}
