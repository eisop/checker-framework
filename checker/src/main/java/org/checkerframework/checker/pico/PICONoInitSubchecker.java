package org.checkerframework.checker.pico;

import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SourceChecker;

import java.util.NavigableSet;
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
    protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
        Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
        checkers.add(InitializationFieldAccessSubchecker.class);
        return checkers;
    }

    @Override
    public NavigableSet<String> getSuppressWarningsPrefixes() {
        NavigableSet<String> result = super.getSuppressWarningsPrefixes();
        result.add("pico");
        return result;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new PICONoInitVisitor(this);
    }
}
