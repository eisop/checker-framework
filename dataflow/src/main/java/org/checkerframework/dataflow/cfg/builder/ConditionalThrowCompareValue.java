package org.checkerframework.dataflow.cfg.builder;

/**
 * Value to compare a formal parameter against when inserting conditional throw edges in the CFG.
 */
public enum ConditionalThrowCompareValue {
    /** Throw when the boolean parameter is true (e.g. assert-false style methods). */
    TRUE,
    /** Throw when the boolean parameter is false (e.g. assert-true style methods). */
    FALSE,
    /** Throw when the reference parameter is null (e.g. {@code @IfNullThrows} on a parameter). */
    NULL
}
