package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.NewClassTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.StringsPlume;

import java.util.*;

/**
 * A node for new object creation.
 *
 * <pre>
 *   <em>new constructor(arg1, arg2, ...)</em>
 *   <em>enclosingExpression.new constructor(arg1, arg2, ...)</em>
 * </pre>
 */
public class ObjectCreationNode extends Node {

    /** The tree for the object creation. */
    protected final NewClassTree tree;

    /** The enclosing type receiver of the object creation, which can be nullable. */
    protected final @Nullable Node receiver;

    /** The constructor node of the object creation. */
    protected final Node constructor;

    /** The arguments of the object creation. */
    protected final List<Node> arguments;

    /** Class body for anonymous classes, otherwise null. */
    protected final @Nullable ClassDeclarationNode classbody;

    /**
     * Constructs a {@link ObjectCreationNode}.
     *
     * @param tree the NewClassTree
     * @param receiver the enclosingType receiver if exists
     * @param constructor the constructor node
     * @param arguments the passed arguments
     * @param classbody the ClassDeclarationNode
     */
    public ObjectCreationNode(
            NewClassTree tree,
            @Nullable Node receiver,
            Node constructor,
            List<Node> arguments,
            @Nullable ClassDeclarationNode classbody) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.constructor = constructor;
        this.arguments = arguments;
        this.classbody = classbody;
        this.receiver = receiver;
    }

    @Pure
    public Node getConstructor() {
        return constructor;
    }

    @Pure
    public List<Node> getArguments() {
        return arguments;
    }

    @Pure
    public Node getArgument(int i) {
        return arguments.get(i);
    }

    /** @return the enclosing type receiver, or null if there is no such receiver */
    @Pure
    public Node getReceiver() {
        return receiver;
    }

    @Pure
    public @Nullable Node getClassBody() {
        return classbody;
    }

    @Override
    @Pure
    public NewClassTree getTree() {
        return tree;
    }

    @Override
    public <R, P> R accept(NodeVisitor<R, P> visitor, P p) {
        return visitor.visitObjectCreation(this, p);
    }

    @Override
    @Pure
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("new " + constructor + "(");
        sb.append(StringsPlume.join(", ", arguments));
        sb.append(")");
        if (classbody != null) {
            // TODO: maybe this can be done nicer...
            sb.append(" ");
            sb.append(classbody.toString());
        }
        return sb.toString();
    }

    @Override
    @Pure
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ObjectCreationNode)) {
            return false;
        }
        ObjectCreationNode other = (ObjectCreationNode) obj;
        if (constructor == null && other.getConstructor() != null) {
            return false;
        }

        return getConstructor().equals(other.getConstructor())
                && getArguments().equals(other.getArguments());
    }

    @Override
    @Pure
    public int hashCode() {
        return Objects.hash(constructor, arguments);
    }

    @Override
    @Pure
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(1 + arguments.size());
        list.add(constructor);
        list.addAll(arguments);
        return list;
    }
}
