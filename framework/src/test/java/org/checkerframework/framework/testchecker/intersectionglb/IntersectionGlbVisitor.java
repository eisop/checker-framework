package org.checkerframework.framework.testchecker.intersectionglb;

import com.sun.source.tree.Tree;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;

import java.util.List;

/** The visitor for {@link IntersectionGlbChecker}. */
public class IntersectionGlbVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

    /**
     * Creates an IntersectionGlbVisitor.
     *
     * @param checker the checker
     */
    public IntersectionGlbVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Does nothing: no written qualifier on an intersection bound is ignored. The summary is the
     * greatest lower bound of the bounds' qualifiers, so it is a subtype of each of them.
     */
    @Override
    protected void checkExplicitAnnotationsOnIntersectionBounds(
            AnnotatedIntersectionType intersection, List<? extends Tree> boundTrees) {}
}
