package org.checkerframework.framework.testchecker.lhsexception;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.util.FlowTestAnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
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
     * <p>Also defaults local variables to the top qualifier, so that a local variable whose type is
     * a type variable gets it unless {@link #getAnnotatedTypeLhsNoTypeVarDefault} is computing its
     * type.
     */
    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defs) {
        super.addCheckedCodeDefaults(defs);
        defs.addCheckedCodeDefault(TOP, TypeUseLocation.LOCAL_VARIABLE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Throws when called from {@link #getAnnotatedTypeLhs} for a variable named {@code
     * throwInLhs}, and when called from {@link #getAnnotatedTypeLhsNoTypeVarDefault} for a variable
     * named {@code throwInLhsNoTypeVarDefault}. A {@link UserError} is reported without the version
     * of the Checker Framework, so the expected output does not depend on it.
     */
    @Override
    protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type) {
        if (isComputingAnnotatedTypeMirrorOfLhs() && tree instanceof VariableTree) {
            String name = ((VariableTree) tree).getName().toString();
            if (name.equals("throwInLhs")) {
                throw new UserError("Simulated error from getAnnotatedTypeLhs.");
            }
            if (name.equals("throwInLhsNoTypeVarDefault") && !getShouldDefaultTypeVarLocals()) {
                throw new UserError("Simulated error from getAnnotatedTypeLhsNoTypeVarDefault.");
            }
        }
        super.addComputedTypeAnnotations(tree, type);
    }
}
