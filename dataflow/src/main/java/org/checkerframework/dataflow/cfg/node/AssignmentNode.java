package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * A node for an assignment:
 *
 * <pre>
 *   <em>variable</em> = <em>expression</em>
 *   <em>expression</em> . <em>field</em> = <em>expression</em>
 *   <em>expression</em> [ <em>index</em> ] = <em>expression</em>
 * </pre>
 *
 * We allow assignments without corresponding AST {@link Tree}s.
 */
public class AssignmentNode extends Node {

    protected final Tree tree;
    protected final Node lhs;
    protected final Node rhs;

    /** Whether the then-store and else-store should be merged. */
    private final boolean mergeStore;

    /**
     * Create an AssignmentNode where, if the transfer result is conditional, the then-store and
     * else-store are always merged.
     *
     * @param tree the {@code AssignmentTree} corresponding to the {@code AssignmentNode}
     * @param target the lhs of {@code tree}
     * @param expression the rhs of {@code tree}
     */
    public AssignmentNode(Tree tree, Node target, Node expression) {
        this(tree, target, expression, true);
    }

    /**
     * Create an AssignmentNode.
     *
     * @param tree the {@code AssignmentTree} corresponding to the {@code AssignmentNode}
     * @param target the lhs of {@code tree}
     * @param expression the rhs of {@code tree}
     * @param mergeStore Should the then-store and else-store be merged?
     */
    public AssignmentNode(Tree tree, Node target, Node expression, boolean mergeStore) {
        super(TreeUtils.typeOf(tree));
        assert tree instanceof AssignmentTree
                || tree instanceof VariableTree
                || tree instanceof CompoundAssignmentTree
                || tree instanceof UnaryTree;
        assert target instanceof FieldAccessNode
                || target instanceof LocalVariableNode
                || target instanceof ArrayAccessNode;
        this.tree = tree;
        this.lhs = target;
        this.rhs = expression;
        this.mergeStore = mergeStore;
    }

    /**
     * Returns the left-hand-side of the assignment.
     *
     * @return the left-hand-side of the assignment
     */
    public Node getTarget() {
        return lhs;
    }

    public Node getExpression() {
        return rhs;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    /** Check if the then-store and else-store should be merged. */
    public boolean shouldMergeStore() {
        return mergeStore;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitAssignment(this, p);
    }

    @Override
    public String toString() {
        return getTarget() + " = " + getExpression();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof AssignmentNode)) {
            return false;
        }
        AssignmentNode other = (AssignmentNode) obj;
        return getTarget().equals(other.getTarget())
                && getExpression().equals(other.getExpression());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTarget(), getExpression());
    }

    @Override
    public Collection<Node> getOperands() {
        return Arrays.asList(getTarget(), getExpression());
    }
}
