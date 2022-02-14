package org.checkerframework.dataflow.busyexpression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

/** A busy expression store contains a set of busy expressions represented by nodes. */
public class BusyExprStore implements Store<BusyExprStore> {

    /** A set of busy expression abstract values. */
    private final Set<BusyExprValue> busyExprValueSet;

    /**
     * Create a new BusyExprStore.
     *
     * @param busyExprValueSet a set of busy expression abstract values
     */
    public BusyExprStore(Set<BusyExprValue> busyExprValueSet) {
        this.busyExprValueSet = busyExprValueSet;
    }

    /** Create a new BusyExprStore. */
    public BusyExprStore() {
        busyExprValueSet = new LinkedHashSet<>();
    }

    /**
     * Kill expressions if they contain variable v.
     *
     * @param v a variable
     */
    public void killBusyExpr(Node v) {
        Iterator<BusyExprValue> iter = busyExprValueSet.iterator();
        while (iter.hasNext()) {
            BusyExprValue busyExprValue = iter.next();
            Node expr = busyExprValue.busyExpression;
            if (isExprContainVariable(expr, v)) {
                iter.remove();
            }
        }
    }

    /**
     * Return if the expression contains variable var.
     *
     * @param expr the expression checked
     * @param var the variable
     * @return true if the expression contains the variable
     */
    public boolean isExprContainVariable(Node expr, Node var) {
        if (expr instanceof NumericalAdditionNode) {
            NumericalAdditionNode addNode = (NumericalAdditionNode) expr;
            return isExprContainVariable(addNode.getLeftOperand(), var)
                    || isExprContainVariable(addNode.getRightOperand(), var);
        } else if (expr instanceof NumericalSubtractionNode) {
            NumericalSubtractionNode subNode = (NumericalSubtractionNode) expr;
            return isExprContainVariable(subNode.getLeftOperand(), var)
                    || isExprContainVariable(subNode.getRightOperand(), var);
        } else if (expr instanceof NumericalMultiplicationNode) {
            NumericalMultiplicationNode mulNode = (NumericalMultiplicationNode) expr;
            return isExprContainVariable(mulNode.getLeftOperand(), var)
                    || isExprContainVariable(mulNode.getRightOperand(), var);
        } else if (expr instanceof IntegerDivisionNode) {
            IntegerDivisionNode divNode = (IntegerDivisionNode) expr;
            return isExprContainVariable(divNode.getLeftOperand(), var)
                    || isExprContainVariable(divNode.getRightOperand(), var);
        }

        return expr.equals(var);
    }

    /**
     * Add busy expression e to busy expression value set.
     *
     * @param e the busy expression to be added
     */
    public void putBusyExpr(BusyExprValue e) {
        busyExprValueSet.add(e);
    }

    /**
     * Add expressions to the store, add sub-expressions to the store recursively
     *
     * @param e the expression to be added
     */
    public void addUseInExpression(Node e) { // todo: local variable?
        if (e instanceof NumericalAdditionNode) {
            BusyExprValue busyExprValue = new BusyExprValue(e);
            putBusyExpr(busyExprValue);
            // recursively add expressions
            NumericalAdditionNode addNode = (NumericalAdditionNode) busyExprValue.busyExpression;
            addUseInExpression(addNode.getLeftOperand());
            addUseInExpression(addNode.getRightOperand());
        } else if (e instanceof NumericalSubtractionNode) {
            BusyExprValue busyExprValue = new BusyExprValue(e);
            putBusyExpr(busyExprValue);
            // recursively add expressions
            NumericalSubtractionNode subNode =
                    (NumericalSubtractionNode) busyExprValue.busyExpression;
            addUseInExpression(subNode.getLeftOperand());
            addUseInExpression(subNode.getRightOperand());
        } else if (e instanceof NumericalMultiplicationNode) {
            BusyExprValue busyExprValue = new BusyExprValue(e);
            putBusyExpr(busyExprValue);
            // recursively add expressions
            NumericalMultiplicationNode mulNode =
                    (NumericalMultiplicationNode) busyExprValue.busyExpression;
            addUseInExpression(mulNode.getLeftOperand());
            addUseInExpression(mulNode.getRightOperand());
        } else if (e instanceof IntegerDivisionNode) {
            BusyExprValue busyExprValue = new BusyExprValue(e);
            putBusyExpr(busyExprValue);
            // recursively add expressions
            IntegerDivisionNode divNode = (IntegerDivisionNode) busyExprValue.busyExpression;
            addUseInExpression(divNode.getLeftOperand());
            addUseInExpression(divNode.getRightOperand());
        }
    }

    @Override
    public BusyExprStore copy() {
        return new BusyExprStore(new HashSet<>(busyExprValueSet));
    }

    @Override
    public BusyExprStore leastUpperBound(BusyExprStore other) {
        Set<BusyExprValue> busyExprValueSetLub = new HashSet<>(this.busyExprValueSet);
        busyExprValueSetLub.retainAll(other.busyExprValueSet);

        return new BusyExprStore(busyExprValueSetLub);
    }

    @Override
    public BusyExprStore widenedUpperBound(BusyExprStore previous) {
        throw new BugInCF("wub of BusyExprStore get called!");
    }

    @Override
    public boolean canAlias(JavaExpression a, JavaExpression b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, BusyExprStore, ?> viz) {
        String key = "busy expressions";
        if (busyExprValueSet.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (BusyExprValue busyExprValue : busyExprValueSet) {
            sjStoreVal.add(busyExprValue.toString());
        }
        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    @Override
    public String toString() {
        return busyExprValueSet.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof BusyExprStore)) {
            return false;
        }
        BusyExprStore other = (BusyExprStore) obj;
        return other.busyExprValueSet.equals(this.busyExprValueSet);
    }

    @Override
    public int hashCode() {
        return this.busyExprValueSet.hashCode();
    }
}
