package org.checkerframework.checker.pico;

import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

import java.util.Set;

public class PICONoInitSubchecker extends BaseTypeChecker {
    public PICONoInitSubchecker() {}

    @Override
    public PICONoInitAnnotatedTypeFactory getTypeFactory() {
        return (PICONoInitAnnotatedTypeFactory) super.getTypeFactory();
    }

    @Override
    protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();
        checkers.add(InitializationFieldAccessSubchecker.class);
        return checkers;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new PICONoInitVisitor(this);
    }
}
