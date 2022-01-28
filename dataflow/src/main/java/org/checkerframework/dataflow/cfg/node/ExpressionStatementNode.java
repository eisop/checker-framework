package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * An ExpressionStatementNode is created for an expression that is used as a statement. An {@code
 * ExpressionStatementNode} is appended to the scanned CFG nodes of the expression. The motivation
 * of this kind of node is to solve the issue
 * https://github.com/typetools/checker-framework/issues/3281
 */
public class ExpressionStatementNode extends Node {
    /** The expression constituting this ExpressionStatementNode. */
    protected final ExpressionTree tree;

    /**
     * Construct a ExpressionStatementNode.
     *
     * @param t the expression constituting this ExpressionStatementNode
     */
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
        return "expression statement " + tree.toString();
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
