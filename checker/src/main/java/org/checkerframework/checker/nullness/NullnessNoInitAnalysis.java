package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/**
 * The analysis class for the non-null type system (serves as factory for the transfer function,
 * stores and abstract values.
 */
public class NullnessNoInitAnalysis
        extends CFAbstractAnalysis<CFValue, NullnessNoInitStore, NullnessNoInitTransfer> {

    /**
     * Creates a new {@code NullnessAnalysis}.
     *
     * @param checker the checker
     * @param factory the factory
     */
    public NullnessNoInitAnalysis(
            BaseTypeChecker checker, NullnessNoInitAnnotatedTypeFactory factory) {
        super(checker, factory);
    }

    @Override
    public NullnessNoInitStore createEmptyStore(boolean sequentialSemantics) {
        return new NullnessNoInitStore(this, sequentialSemantics);
    }

    @Override
    public NullnessNoInitStore createCopiedStore(NullnessNoInitStore s) {
        return new NullnessNoInitStore(s);
    }

    @Override
    public @Nullable CFValue createAbstractValue(
            AnnotationMirrorSet annotations, TypeMirror underlyingType) {
        return defaultCreateAbstractValue(this, annotations, underlyingType);
    }
}
