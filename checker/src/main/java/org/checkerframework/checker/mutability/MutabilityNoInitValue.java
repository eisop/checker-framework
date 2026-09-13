package org.checkerframework.checker.mutability;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/** The abstract value for the mutability type system. */
public class MutabilityNoInitValue extends CFAbstractValue<MutabilityNoInitValue> {
    /**
     * Create a new MutabilityNoInitValue.
     *
     * @param analysis the analysis
     * @param annotations the annotations
     * @param underlyingType the underlying type
     */
    public MutabilityNoInitValue(
            CFAbstractAnalysis<MutabilityNoInitValue, ?, ?> analysis,
            AnnotationMirrorSet annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }
}
