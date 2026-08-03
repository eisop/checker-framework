package org.checkerframework.dataflow.cfg.builder;

import javax.lang.model.type.TypeMirror;

/**
 * One parameter position that may cause the callee to throw when the argument compares equal to
 * {@link #compareValue}.
 */
public class ParameterConditionalThrowSpec {
    /** 0-based parameter index. */
    public final int parameterIndex;

    /** Value to compare against; throw when (argument equals this comparison kind). */
    public final ConditionalThrowCompareValue compareValue;

    /** Exception type thrown when the condition holds. */
    public final TypeMirror exceptionType;

    /**
     * Creates a spec.
     *
     * @param parameterIndex 0-based index
     * @param compareValue comparison kind
     * @param exceptionType exception type thrown when the condition holds
     */
    public ParameterConditionalThrowSpec(
            int parameterIndex,
            ConditionalThrowCompareValue compareValue,
            TypeMirror exceptionType) {
        this.parameterIndex = parameterIndex;
        this.compareValue = compareValue;
        this.exceptionType = exceptionType;
    }
}
