package org.checkerframework.checker.tainting;

import com.sun.source.tree.Tree;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

/** Visitor for the {@link TaintingChecker}. */
public class TaintingVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

    /**
     * Creates a {@link TaintingVisitor}.
     *
     * @param checker the checker that uses this visitor
     */
    public TaintingVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Don't check that the constructor result is top. Checking that the super() or this() call is a
     * subtype of the constructor result is sufficient.
     */
    @Override
    protected void checkConstructorResult(
            AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {}

    @Override
    protected void checkExtendsOrImplements(
            Tree boundClause,
            AnnotationMirrorSet classBounds,
            TypeMirror classType,
            boolean isExtends) {
        // Tainting checker allows annotation on super type.
        checkExtendsOrImplements(boundClause, classBounds, classType, isExtends, false);
    }
}
