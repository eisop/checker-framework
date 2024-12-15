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
    protected final Tree tree;

    /**
     * Create a new ClassLiteralNode.
     *
     * @param tree the class literal
     */
    public ClassLiteralNode(MemberSelectTree tree) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
    }

    @Override
    public @Nullable Tree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitClassLiteral(this, p);
    }

    @Override
    public Collection<Node> getOperands() {
        return Collections.emptyList();
    }
}
