package org.checkerframework.framework.testchecker.skipdefs;

import com.sun.source.tree.VariableTree;

import org.checkerframework.common.basetype.BaseTypeChecker;

/** A test checker that skips the variable declarations whose names start with "skipped". */
public class SkipDefsChecker extends BaseTypeChecker {

    @Override
    public boolean shouldSkipDefs(VariableTree tree) {
        return tree.getName().toString().startsWith("skipped") || super.shouldSkipDefs(tree);
    }
}
