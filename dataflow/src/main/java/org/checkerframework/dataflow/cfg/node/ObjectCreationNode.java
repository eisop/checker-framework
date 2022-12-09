package org.checkerframework.dataflow.cfg.node;

import com.sun.source.tree.NewClassTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.StringsPlume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

    /** The receiver of the object creation or null. */
    protected final @Nullable Node enclosingExpression;

    /** The constructor node of the object creation. */
    // TODO: Change its name to identifier in a separate pr
    protected final Node constructor;

    /** The arguments of the object creation. */
    protected final List<Node> arguments;

    /** Class body for anonymous classes, otherwise null. */
    protected final @Nullable ClassDeclarationNode classbody;

    /**
     * Constructs a {@link ObjectCreationNode}.
     *
     * @param tree the NewClassTree
     * @param enclosingExpr the enclosingType Node if exists
     * @param constructor the constructor node
     * @param arguments the passed arguments
     * @param classbody the ClassDeclarationNode
     */
    public ObjectCreationNode(
            NewClassTree tree,
            @Nullable Node enclosingExpr,
            Node constructor,
            List<Node> arguments,
            @Nullable ClassDeclarationNode classbody) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.enclosingExpression = enclosingExpr;
        this.constructor = constructor;
        this.arguments = arguments;
        this.classbody = classbody;
    }

    /**
     * Returns the constructor node
     *
     * @return the constructor node
     */
    @Pure
    public Node getConstructor() {
        return constructor;
    }

    /**
     * Returns the explicit arguments to the object creation
     *
     * @return the arguments
     */
    @Pure
    public List<Node> getArguments() {
        return arguments;
    }

    /**
     * Returns the i-th explicit argument to the object creation
     *
     * @param i the index of the argument
     * @return the argument
     */
    @Pure
    public Node getArgument(int i) {
        return arguments.get(i);
    }

    /**
     * Returns the receiver, the receiver only exists if the object creation is from an inner class
     *
     * @return the enclosing type receiver
     */
    @Pure
    public @Nullable Node getEnclosingExpression() {
        return enclosingExpression;
    }

    /**
     * Returns the classbody
     *
     * @return the classbody
     */
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

    /**
     * If the object is from an inner class, to make it clear, add enclosingExpr as its first
     * argument explicitly.
     */
    @Override
    @SideEffectFree
    public String toString() {
        StringBuilder sb = new StringBuilder();
        List<Node> argumentsDeepCopy = new ArrayList<Node>();
        int startingIndex = 0;
        if (enclosingExpression != null) {
            sb.append(enclosingExpression + ".");
            // Remove the first argument if there is a receiver
            startingIndex = 1;
        }
        for (int i = startingIndex; i < arguments.size(); i++) {
            argumentsDeepCopy.add(arguments.get(i));
        }
        sb.append("new " + constructor + "(");

        sb.append(StringsPlume.join(", ", argumentsDeepCopy));
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
        // TODO: maybe it is easier to just compare the new class tree.
        if (constructor == null && other.getConstructor() != null) {
            return false;
        }

        return getConstructor().equals(other.getConstructor())
                && getArguments().equals(other.getArguments())
                && (getEnclosingExpression() == null
                        ? null == other.getEnclosingExpression()
                        : getEnclosingExpression().equals(other.getEnclosingExpression()));
    }

    @Override
    @SideEffectFree
    public int hashCode() {
        return Objects.hash(enclosingExpression, constructor, arguments);
    }

    @Override
    @Pure
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(1 + arguments.size());
        if (enclosingExpression != null) {
            list.add(enclosingExpression);
        }
        list.add(constructor);
        list.addAll(arguments);
        return list;
    }
}
