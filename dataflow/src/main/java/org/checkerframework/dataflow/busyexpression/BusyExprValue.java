package org.checkerframework.dataflow.busyexpression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.BinaryOperationNode;
import org.checkerframework.javacutil.BugInCF;

/**
 * BusyExprValue class contains a BinaryOperationNode. So we only consider expressions that are in
 * form of BinaryOperationNode: <em>lefOperandNode</em> <em>operator</em> <em>rightOperandNode</em>
 */
public class BusyExprValue implements AbstractValue<BusyExprValue> {

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
    public BusyExprValue leastUpperBound(BusyExprValue other) {
        throw new BugInCF("lub of BusyExpValue get called!");
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
        // It's preferred to use "==" to compare two nodes in checker framework,
        // but in this case we use `equals` to only measure value equality.
        // If we use "==", two expressions from different nodes with same
        // abstract values will not consider as the same and cause the analysis
        // incorrect. Hence we use `equals` in place of `==`.
        // It looks like using `equals` is faulty. Suppose we have 2 expressions
        // `b + foo()` both in the `if` and `else` branches, where calling `foo()`
        // returns different values each time. It seems that our program will analyze
        // the 2 expressions as the same and make it as the busy expression at the
        // conditional block. However, foo() is a method invocation, and calling
        // it will introduce an exceptional block follows it. As the result,
        // there are at least 2 blocks following foo() block, one with a transfer
        // result `b + foo()`, another is the exception block with empty transfer
        // result. Their intersection is empty when propagating to the foo() block.
        // The abstract value `b+foo()` is lost during backward propagating, hence
        // it's not a problem in this case.
        return this.busyExpression.equals(other.busyExpression);
    }
}
