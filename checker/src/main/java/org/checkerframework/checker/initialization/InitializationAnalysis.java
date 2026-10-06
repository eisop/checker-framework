package org.checkerframework.checker.initialization;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/**
 * The analysis class for the initialization type system (serves as factory for the transfer
 * function, stores, and abstract values.
 */
public class InitializationAnalysis
        extends CFAbstractAnalysis<CFValue, InitializationStore, InitializationTransfer> {

    /**
     * Creates a new {@code InitializationAnalysis}.
     *
     * @param checker the checker
     * @param factory the factory
     */
    protected InitializationAnalysis(
            BaseTypeChecker checker, InitializationParentAnnotatedTypeFactory factory) {
        super(checker, factory);
    }

    @Override
    public InitializationStore createEmptyStore(boolean sequentialSemantics) {
        return new InitializationStore(this, sequentialSemantics);
    }

    @Override
    public InitializationStore createCopiedStore(InitializationStore s) {
        return new InitializationStore(s);
    }

    @Override
    public @Nullable CFValue createAbstractValue(
            AnnotationMirrorSet annotations, TypeMirror underlyingType) {
        return defaultCreateAbstractValue(this, annotations, underlyingType);
    }

    @Override
    public InitializationParentAnnotatedTypeFactory getTypeFactory() {
        return (InitializationParentAnnotatedTypeFactory) super.getTypeFactory();
    }
}
