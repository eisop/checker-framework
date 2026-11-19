package org.checkerframework.framework.testchecker.skipdefs;

import com.sun.source.tree.VariableTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * Reports a diagnostic on each visited variable declaration whose name ends with "Reported", to
 * test that skipping a variable also skips an override of {@code visitVariable}.
 */
public class SkipDefsVisitor extends BaseTypeVisitor<SkipDefsAnnotatedTypeFactory> {

    /**
     * Creates a new SkipDefsVisitor.
     *
     * @param checker the checker
     */
    public SkipDefsVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitVariable(VariableTree tree, Void p) {
        if (tree.getName().toString().endsWith("Reported")) {
            // "type.invalid" is a built-in error key that this test misuses.
            checker.reportError(tree, "type.invalid", "mock type", "mock message");
        }
        return super.visitVariable(tree, p);
    }
}
