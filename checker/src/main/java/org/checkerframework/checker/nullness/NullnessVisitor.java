package org.checkerframework.checker.nullness;

import com.sun.source.util.TreePath;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

public class NullnessVisitor extends BaseTypeVisitor<NullnessAnnotatedTypeFactory> {

    public NullnessVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public void visit(TreePath path) {
        // do nothing
    }
}
