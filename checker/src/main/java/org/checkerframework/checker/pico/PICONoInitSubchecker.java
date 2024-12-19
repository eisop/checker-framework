package org.checkerframework.checker.pico;

import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

import java.util.Set;

/** The PICO subchecker. */
public class PICONoInitSubchecker extends BaseTypeChecker {
    /** Create a new PICONoInitSubchecker. */
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
