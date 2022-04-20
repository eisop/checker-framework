package org.checkerframework.dataflow.busyexpression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;

/**
 * BusyExprValue class contains a BinaryOperationNode. So we only consider expressions that are in
 * form of BinaryOperationNode: <em>lefOperandNode</em> <em>operator</em> <em>rightOperandNode</em>.
 */
public class BusyExprValue {

    /**
     * A busy expression is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.BinaryOperationNode}
     */
    protected final BinaryOperationNode busyExpression;

    /**
     * Create a new busy expression.
     *
     * @param n a node
     */
    public BusyExprValue(BinaryOperationNode n) {
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
        // Use `equals` to check equality rather than using `==`.
        return this.busyExpression.equals(other.busyExpression);
    }
}
