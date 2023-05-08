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
 *   <em>new identifier(arg1, arg2, ...)</em>
 *   <em>new identifier&lt;T&gt;(arg1, arg2, ...)</em>
 *   <em>enclosingExpression.new identifier(arg1, arg2, ...)</em>
 * </pre>
 */
public class ObjectCreationNode extends Node {

    /** The tree for the object creation. */
    protected final NewClassTree tree;

    /** The enclosing expression of the object creation or null. */
    protected final @Nullable Node enclosingExpression;

    /**
     * The identifier node of the object creation. A non-generic constructor can refer to a
     * ClassNameNode, while a generic constructor identifier can refer to a ParameterizedTypeNode.
     */
    protected final Node identifier;

    /** The arguments of the object creation. */
    protected final List<Node> arguments;

    /** Class body for anonymous classes, otherwise null. */
    protected final @Nullable ClassDeclarationNode classbody;

    /**
     * Constructs a {@link ObjectCreationNode}.
     *
     * @param tree the NewClassTree
     * @param enclosingExpr the enclosing expression Node if it exists, or null
     * @param identifier the identifier node
     * @param arguments the passed arguments
     * @param classbody the ClassDeclarationNode
     */
    public ObjectCreationNode(
            NewClassTree tree,
            @Nullable Node enclosingExpr,
            Node identifier,
            List<Node> arguments,
            @Nullable ClassDeclarationNode classbody) {
        super(TreeUtils.typeOf(tree));
        this.tree = tree;
        this.enclosingExpression = enclosingExpr;
        this.identifier = identifier;
        this.arguments = arguments;
        this.classbody = classbody;
    }

    /**
     * Returns the constructor node.
     *
     * @return the constructor node
     * @deprecated use {@link #getIdentifier()}
     */
    @Pure
    @Deprecated
    public Node getConstructor() {
        return identifier;
    }

    /**
     * Returns the identifier node. A non-generic constructor can refer to a ClassNameNode, while a
     * generic constructor identifier can refer to a ParameterizedTypeNode.
     *
     * @return the identifier node
     */
    @Pure
    public Node getIdentifier() {
        return identifier;
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
     * Returns the enclosing expression node, which only exists if it is an inner class
     * instantiation
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
        if (enclosingExpression != null) {
            sb.append(enclosingExpression + ".");
        }
        sb.append("new " + identifier + "(");
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
        // TODO: See issue 376
        if (identifier == null && other.getIdentifier() != null) {
            return false;
        }

        return getIdentifier().equals(other.getIdentifier())
                && getArguments().equals(other.getArguments())
                && (getEnclosingExpression() == null
                        ? null == other.getEnclosingExpression()
                        : getEnclosingExpression().equals(other.getEnclosingExpression()));
    }

    @Override
    @SideEffectFree
    public int hashCode() {
        return Objects.hash(enclosingExpression, identifier, arguments);
    }

    @Override
    @SideEffectFree
    public Collection<Node> getOperands() {
        ArrayList<Node> list = new ArrayList<>(2 + arguments.size());
        if (enclosingExpression != null) {
            list.add(enclosingExpression);
        }
        list.add(identifier);
        list.addAll(arguments);
        return list;
    }
}
