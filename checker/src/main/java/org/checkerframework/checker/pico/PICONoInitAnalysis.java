package org.checkerframework.checker.pico;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/**
 * The analysis class for the immutability type system (serves as factory for the transfer function,
 * stores and abstract values.
 */
public class PICONoInitAnalysis
        extends CFAbstractAnalysis<PICONoInitValue, PICONoInitStore, PICONoInitTransfer> {

    public PICONoInitAnalysis(BaseTypeChecker checker, PICONoInitAnnotatedTypeFactory factory) {
        super(checker, factory, -1);
    }

    @Override
    public PICONoInitStore createEmptyStore(boolean sequentialSemantics) {
        return new PICONoInitStore(this, sequentialSemantics);
    }

    @Override
    public PICONoInitStore createCopiedStore(PICONoInitStore picoNoInitStore) {
        return new PICONoInitStore(picoNoInitStore);
    }

    @Override
    public PICONoInitValue createAbstractValue(
            AnnotationMirrorSet annotations, TypeMirror underlyingType) {
        if (!CFAbstractValue.validateSet(annotations, underlyingType, atypeFactory)) {
            return null;
        }
        return new PICONoInitValue(this, annotations, underlyingType);
    }
}
