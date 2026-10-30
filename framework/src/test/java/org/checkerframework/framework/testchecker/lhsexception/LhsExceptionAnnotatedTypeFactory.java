package org.checkerframework.framework.testchecker.lhsexception;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.testchecker.util.FlowTestAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.UserError;

/** The type factory of the {@link LhsExceptionChecker}. */
public class LhsExceptionAnnotatedTypeFactory extends FlowTestAnnotatedTypeFactory {

    /**
     * Creates a LhsExceptionAnnotatedTypeFactory.
     *
     * @param checker the checker
     */
    public LhsExceptionAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Throws when called from {@link #getAnnotatedTypeLhs} for a variable named {@code
     * throwInLhs}. A {@link UserError} is reported without the version of the Checker Framework, so
     * the expected output does not depend on it.
     */
    @Override
    protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type) {
        if (isComputingAnnotatedTypeMirrorOfLhs()
                && tree instanceof VariableTree
                && ((VariableTree) tree).getName().contentEquals("throwInLhs")) {
            throw new UserError("Simulated error from getAnnotatedTypeLhs.");
        }
        super.addComputedTypeAnnotations(tree, type);
    }
}
