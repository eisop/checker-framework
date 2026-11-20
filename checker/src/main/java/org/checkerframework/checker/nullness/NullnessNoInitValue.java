package org.checkerframework.checker.nullness;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import javax.lang.model.type.TypeMirror;

/**
 * The abstract value of the Nullness Checker's dataflow analysis. It behaves just like {@link
 * CFValue}; whether {@code @PolyNull} is known to be {@code @NonNull} or {@code @Nullable} is
 * tracked by {@link NullnessNoInitStore} and resolved in each value by {@link
 * NullnessNoInitTransfer}.
 */
public class NullnessNoInitValue extends CFAbstractValue<NullnessNoInitValue> {

    /**
     * Creates a new NullnessNoInitValue.
     *
     * @param analysis the analysis
     * @param annotations the annotations
     * @param underlyingType the underlying type
     */
    public NullnessNoInitValue(
            CFAbstractAnalysis<NullnessNoInitValue, ?, ?> analysis,
            AnnotationMirrorSet annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }
}
