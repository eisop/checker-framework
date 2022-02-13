package org.checkerframework.dataflow.busyexpression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;

public class BusyExprValue implements AbstractValue<BusyExprValue> {

    /**
     * A busy expression is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.NumericalAdditionNode} or {@link
     * org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode} or {@link
     * org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode} or {@link
     * org.checkerframework.dataflow.cfg.node.IntegerDivisionNode}
     */
    protected final Node busyExpression;

    @Override
    public BusyExprValue leastUpperBound(BusyExprValue other) {
        throw new BugInCF("lub of BusyExpValue get called!");
    }

    /**
     * Create a new busy expression.
     *
     * @param n a node
     */
    public BusyExprValue(Node n) {
        this.busyExpression = n;
    }

    @Override
    public String toString() {
        return this.busyExpression.toString();
    }

    @Override
    public int hashCode() {
        return this.busyExpression.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof BusyExprValue)) {
            return false;
        }
        BusyExprValue other = (BusyExprValue) obj;
        return this.busyExpression.equals(other.busyExpression);
    }
}
