package org.checkerframework.checker.immutability;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/** Created by mier on 15/08/17. */
public class PICONoInitValue extends CFAbstractValue<PICONoInitValue> {
    public PICONoInitValue(
            CFAbstractAnalysis<PICONoInitValue, ?, ?> analysis,
            AnnotationMirrorSet annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }
}
