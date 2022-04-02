package org.checkerframework.dataflow.analysis;

import org.checkerframework.javacutil.BugInCF;

/**
 * UnusedAbstractValue is an AbstractValue but is not involved in any lub computation during dataflow
 * analysis. For those analyses which handle lub computation at a higher level (e.g., store level), it is
 * sufficient to use UnusedAbstractValue and unnecessary to implement another specific
 * AbstractValue. Example analysis using UnusedAbstractValue is LiveVariable analysis. This is a
 * workaround for issue https://github.com/eisop/checker-framework/issues/200
 */
public class UnusedAbstractValue implements AbstractValue<UnusedAbstractValue> {

    @Override
    public UnusedAbstractValue leastUpperBound(UnusedAbstractValue other) {
        throw new BugInCF("UnusedAbstractValue.leastUpperBound was called!");
    }
}
