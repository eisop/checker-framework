package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.Collections;

/** A node for a class literal. For example: {@code String.class}. */
public class ClassLiteralNode extends Node {
    /** The tree for the class literal. */
    protected final MemberSelectTree tree;

    /** The class name of class literal */
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
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
