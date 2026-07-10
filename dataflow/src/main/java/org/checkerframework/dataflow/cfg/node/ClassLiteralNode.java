package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/** A node for a class literal. For example: {@code String.class}. */
public class ClassLiteralNode extends Node {
    /** The tree for the class literal. */
    protected final MemberSelectTree tree;

    /** The class name of the class literal. */
    protected final Node className;

    /**
     * Create a new ClassLiteralNode.
     *
     * @param tree the class literal
     * @param className the class name for the class literal
     */
    public ClassLiteralNode(MemberSelectTree tree, Node className) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.className = className;
    }

    @Override
    public @Nullable Tree getTree() {
        return tree;
    }

    /**
     * Get the class name of the class literal.
     *
     * @return the class name of the class literal
     */
    public Node getClassName() {
        return className;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitClassLiteral(this, p);
    }

    @Override
    public String toString() {
        return tree.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClassLiteralNode)) {
            return false;
        }
        ClassLiteralNode other = (ClassLiteralNode) obj;
        return getClassName().equals(other.getClassName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClassName());
    }

    @Override
    @SideEffectFree
    public Collection<Node> getOperands() {
        return Collections.singleton(className);
    }
}
