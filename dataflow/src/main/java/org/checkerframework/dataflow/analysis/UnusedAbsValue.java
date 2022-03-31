package org.checkerframework.dataflow.analysis;

import org.checkerframework.javacutil.BugInCF;

public class UnusedAbsValue implements AbstractValue<UnusedAbsValue> {

    @Override
    public UnusedAbsValue leastUpperBound(UnusedAbsValue other) {
        throw new BugInCF("lub of UnusedAbsValue gets called!");
    }
}
