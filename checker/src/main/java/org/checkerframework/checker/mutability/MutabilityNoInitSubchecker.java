package org.checkerframework.checker.mutability;

import org.checkerframework.checker.initialization.InitializationFieldAccessSubchecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SourceChecker;

import java.util.NavigableSet;
import java.util.Set;

/** The no-initialization subchecker for the mutability checker. */
public class MutabilityNoInitSubchecker extends BaseTypeChecker {
    /** Create a new MutabilityNoInitSubchecker. */
    public MutabilityNoInitSubchecker() {}

    @Override
    public MutabilityNoInitAnnotatedTypeFactory getTypeFactory() {
        return (MutabilityNoInitAnnotatedTypeFactory) super.getTypeFactory();
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
        result.add("immutability");
        return result;
    }

    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new MutabilityNoInitVisitor(this);
    }
}
