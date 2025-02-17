package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * A node for member references and lambdas.
 *
 * <p>The {@link Node#type} of a FunctionalInterfaceNode is determined by the assignment context the
 * member reference or lambda is used in.
 *
 * <pre>
 *   <em>FunctionalInterface func = param1, param2, ... &rarr; statement</em>
 * </pre>
 *
 * <pre>
 *   <em>FunctionalInterface func = param1, param2, ... &rarr; { ... }</em>
 * </pre>
 *
 * <pre>
 *   <em>FunctionalInterface func = member reference</em>
 * </pre>
 */
public class FunctionalInterfaceNode extends Node {

    protected final Tree tree;

    public FunctionalInterfaceNode(MemberReferenceTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
    }

    public FunctionalInterfaceNode(LambdaExpressionTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
    }

    @Override
    public Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitMemberReference(this, p);
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FunctionalInterfaceNode that = (FunctionalInterfaceNode) o;

        return tree != null ? tree.equals(that.tree) : that.tree == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tree);
    }

    @Override
    @SideEffectFree
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
