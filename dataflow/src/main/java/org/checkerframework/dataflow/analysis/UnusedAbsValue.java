package org.checkerframework.dataflow.analysis;

import org.checkerframework.javacutil.BugInCF;

/**
 * UnusedAbsValue is an AbstractValue but is not involved in any computation during dataflow
 * analysis. For those analyses which have no computation between two AbstractValues, it is
 * sufficient to use UnusedAbsValue and unnecessary to implement another specific AbstractValue.
 * Example analysis using UnusedAbsValue is LiveVariable analysis.
 */
public class UnusedAbsValue implements AbstractValue<UnusedAbsValue> {

    @Override
    public UnusedAbsValue leastUpperBound(UnusedAbsValue other) {
        throw new BugInCF("lub of UnusedAbsValue gets called!");
    }
}
