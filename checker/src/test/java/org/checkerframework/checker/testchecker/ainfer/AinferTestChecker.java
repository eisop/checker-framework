package org.checkerframework.checker.testchecker.ainfer;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SourceChecker;

import java.util.Set;

/**
 * Checker for a simple type system to test whole-program inference. Uses the Value Checker as a
 * subchecker to ensure that generated files contain annotations both from this checker and from the
 * Value Checker, to make certain that subchecker outputs aren't overwritten.
 */
public class AinferTestChecker extends BaseTypeChecker {

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new AinferTestVisitor(this);
    }

    @Override
    protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);
        return checkers;
    }
}
