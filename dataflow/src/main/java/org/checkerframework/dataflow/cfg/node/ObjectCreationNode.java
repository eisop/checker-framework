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

    /** The enclosing expression of the object creation or null. */
    protected final @Nullable Node enclosingExpression;

    // TODO: See issue 376
    /** The constructor node of the object creation. */
    protected final Node constructor;

    /** The arguments of the object creation. */
    // TODO: explain the optional receiver
    protected final List<Node> arguments;

    /** Class body for anonymous classes, otherwise null. */
    protected final @Nullable ClassDeclarationNode classbody;

    /**
     * Constructs a {@link ObjectCreationNode}.
     *
     * @param tree the NewClassTree
     * @param enclosingExpr the enclosing expression Node if it exists, or null
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
<<<<<<< HEAD
        this.enclosingExpression = enclosingExpr;
=======
        this.receiver = receiver;
>>>>>>> 2c8db9ab9 (fix some comments from Werner, but this is not the final version, need type checking related code)
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
<<<<<<< HEAD
     * Returns the enclosing expression node, which only exists if it is an inner class
     * instantiation
=======
     * Returns the receiver, the receiver only exists if the object creation is from an inner class
>>>>>>> 2c8db9ab9 (fix some comments from Werner, but this is not the final version, need type checking related code)
     *
     * @return the enclosing type expression node
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

    @Override
    @SideEffectFree
    public String toString() {
        StringBuilder sb = new StringBuilder();
<<<<<<< HEAD
        // To serve the purpose of cfg presentation, set the first argument to enclosing expression
        // explicitly.
        if (enclosingExpression != null) {
            sb.append(enclosingExpression + ".");
=======
        List<Node> argumentsDeepCopy = new ArrayList<Node>();
        int startingIndex = 0;
        if (receiver != null) {
            sb.append(receiver + ".");
            // Remove the first argument if there is a receiver
            startingIndex = 1;
        }
        for (int i = startingIndex; i < arguments.size(); i++) {
            argumentsDeepCopy.add(arguments.get(i));
>>>>>>> 2c8db9ab9 (fix some comments from Werner, but this is not the final version, need type checking related code)
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
        // TODO: See issue 376
        if (constructor == null && other.getConstructor() != null) {
            return false;
        }
        if ((receiver != null && other.getReceiver() == null)
                || (receiver == null && other.getReceiver() != null)
                || (receiver != null && !getReceiver().equals(other.getReceiver()))) {
            return false;
        }

        return getConstructor().equals(other.getConstructor())
<<<<<<< HEAD
                && getArguments().equals(other.getArguments())
                && (getEnclosingExpression() == null
                ? null == other.getEnclosingExpression()
                : getEnclosingExpression().equals(other.getEnclosingExpression()));
=======
                && getArguments().equals(other.getArguments());
    }

    @Override
    @SideEffectFree
    public int hashCode() {
        int result;
        if (receiver != null) {
            result = Objects.hash(receiver, constructor, arguments);
        } else {
            result = Objects.hash(constructor, arguments);
        }
        return result;
>>>>>>> 2c8db9ab9 (fix some comments from Werner, but this is not the final version, need type checking related code)
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
<<<<<<< HEAD
        if (enclosingExpression != null) {
            list.add(enclosingExpression);
=======
        if (receiver != null) {
            list.add(receiver);
>>>>>>> 2c8db9ab9 (fix some comments from Werner, but this is not the final version, need type checking related code)
        }
        list.add(constructor);
        list.addAll(arguments);
        return list;
    }
}
