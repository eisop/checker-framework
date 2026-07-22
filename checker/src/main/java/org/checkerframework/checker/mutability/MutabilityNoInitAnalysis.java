package org.checkerframework.checker.mutability;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/**
 * The analysis class for the mutability type system. It serves as the factory for transfer
 * functions, stores, and abstract values.
 */
public class MutabilityNoInitAnalysis
        extends CFAbstractAnalysis<
                MutabilityNoInitValue, MutabilityNoInitStore, MutabilityNoInitTransfer> {
    /**
     * Create MutabilityNoInitAnalysis.
     *
     * @param checker the BaseTypeChecker this analysis works with
     * @param factory the MutabilityNoInitAnnotatedTypeFactory this analysis works with
     */
    public MutabilityNoInitAnalysis(
            BaseTypeChecker checker, MutabilityNoInitAnnotatedTypeFactory factory) {
        super(checker, factory, -1);
    }

    @Override
    public MutabilityNoInitStore createEmptyStore(boolean sequentialSemantics) {
        return new MutabilityNoInitStore(this, sequentialSemantics);
    }

    @Override
    public MutabilityNoInitStore createCopiedStore(MutabilityNoInitStore store) {
        return new MutabilityNoInitStore(store);
    }

    @Override
    public MutabilityNoInitValue createAbstractValue(
            AnnotationMirrorSet annotations, TypeMirror underlyingType) {
        if (!CFAbstractValue.validateSet(annotations, underlyingType, atypeFactory)) {
            return null;
        }
        return new MutabilityNoInitValue(this, annotations, underlyingType);
    }
}
