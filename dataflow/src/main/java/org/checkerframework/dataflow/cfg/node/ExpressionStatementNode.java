package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * A node for the purpose of merging store in dataflow analysis. It usually follows an expression
 * statement.
 *
 * <p>Note: Does not represent any AST structure.
 */
public class ExpressionStatementNode extends Node {
    /** The expression precedes this MergeOfStoreNode. */
    protected final ExpressionTree tree;

    /** @param t the expression precedes this MergeOfStoreNode */
    public ExpressionStatementNode(ExpressionTree t) {
        super(TreeUtils.typeOf(t));
        tree = t;
    }

    @Override
    public @Nullable Tree getTree() {
        return null;
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitExpressionStatement(this, p);
    }

    @Override
    public String toString() {
        return "merge store after " + tree.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }
}
