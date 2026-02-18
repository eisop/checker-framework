package org.checkerframework.checker.pico;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/** The abstract value for the immutability type system. */
public class PICONoInitValue extends CFAbstractValue<PICONoInitValue> {
    /**
     * Create a new PICONoInitValue.
     *
     * @param analysis the analysis
     * @param annotations the annotations
     * @param underlyingType the underlying type
     */
    public PICONoInitValue(
            CFAbstractAnalysis<PICONoInitValue, ?, ?> analysis,
            AnnotationMirrorSet annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }
}
