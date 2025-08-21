package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Determines the nullness type of calls to {@link java.util.Collection#toArray()}.
 *
 * @checker_framework.manual #nullness-collection-toarray Nullness and conversions from collections
 *     to arrays
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
public class CollectionToArrayHeuristics {

    /** The processing environment. */
    private final ProcessingEnvironment processingEnv;

    /** The checker, used for issuing diagnostic messages. */
    private final BaseTypeChecker checker;

    /** The type factory. */
    private final NullnessNoInitAnnotatedTypeFactory atypeFactory;

    /** Whether to trust {@code @ArrayLen(0)} annotations. */
    private final boolean trustArrayLenZero;

    /** The Collection type. */
    private final AnnotatedDeclaredType collectionType;

    /** The Collection.toArray(T[]) method. */
    private final ExecutableElement collectionToArrayE;

    /** The Collection.size() method. */
    private final ExecutableElement size;

    /** The ArrayLen.value field/element. */
    private final ExecutableElement arrayLenValueElement;

    /**
     * Create a CollectionToArrayHeuristics.
     *
     * @param checker the checker, used for issuing diagnostic messages
     * @param factory the type factory
     */
    public CollectionToArrayHeuristics(
            BaseTypeChecker checker, NullnessNoInitAnnotatedTypeFactory factory) {
        this.processingEnv = checker.getProcessingEnvironment();
        this.checker = checker;
        this.atypeFactory = factory;

        this.collectionType =
                factory.fromElement(ElementUtils.getTypeElement(processingEnv, Collection.class));
        this.collectionToArrayE =
                TreeUtils.getMethod("java.util.Collection", "toArray", processingEnv, "T[]");
        this.size = TreeUtils.getMethod("java.util.Collection", "size", 0, processingEnv);
        this.arrayLenValueElement = TreeUtils.getMethod(ArrayLen.class, "value", 0, processingEnv);

        this.trustArrayLenZero =
                checker.getLintOption(
                        NullnessChecker.LINT_TRUSTARRAYLENZERO,
                        NullnessChecker.LINT_DEFAULT_TRUSTARRAYLENZERO);
    }

    /**
     * If the method invocation is a call to {@code toArray}, then it manipulates the returned type
     * of {@code method} arg to contain the appropriate nullness. Otherwise, it does nothing.
     *
     * @param tree method invocation tree
     * @param method invoked method type
     */
    public void handle(MethodInvocationTree tree, AnnotatedExecutableType method) {
        if (TreeUtils.isMethodInvocation(tree, collectionToArrayE, processingEnv)) {
            assert !tree.getArguments().isEmpty() : tree;
            ExpressionTree argument = tree.getArguments().get(0);
            boolean receiverIsNonNull = receiverIsCollectionOfNonNullElements(tree);
            boolean argIsHandled =
                    isRecognizedToArrayArg(argument, receiverName(tree.getMethodSelect()))
                            || (trustArrayLenZero && isArrayLenZeroFieldAccess(argument));
            setComponentNullness(receiverIsNonNull && argIsHandled, method.getReturnType());

            // TODO: We need a mechanism to prevent nullable collections
            // from inserting null elements into a nonnull arrays.
            if (!receiverIsNonNull) {
                setComponentNullness(false, method.getParameterTypes().get(0));
            }

            if (receiverIsNonNull && !argIsHandled) {
                if (!(argument instanceof NewArrayTree)) {
                    checker.reportWarning(tree, "toarray.nullable.elements.not.newarray");
                } else {
                    checker.reportWarning(tree, "toarray.nullable.elements.mismatched.size");
                }
            }
        }
    }

    /**
     * Sets the nullness of the component of the array type.
     *
     * @param isNonNull indicates which annotation ({@code NonNull} or {@code Nullable}) should be
     *     inserted
     * @param type the array type
     */
    private void setComponentNullness(boolean isNonNull, AnnotatedTypeMirror type) {
        assert type.getKind() == TypeKind.ARRAY;
        AnnotatedTypeMirror compType = ((AnnotatedArrayType) type).getComponentType();
        compType.replaceAnnotation(isNonNull ? atypeFactory.NONNULL : atypeFactory.NULLABLE);
    }

    /**
     * Returns {@code true} if {@code argument} is an array argument to {@link
     * Collection#toArray(Object[]) toArray(T[])} that this heuristic recognizes as producing a
     * non-null array of the element type.
     *
     * <p>Recognized forms include:
     *
     * <ul>
     *   <li>{@code new T[] { }} (empty array literal)
     *   <li>{@code new T[0]} (zero-length allocation)
     *   <li>{@code new T[receiver.size()]} where {@code receiver} is the {@code Collection} on
     *       which {@code toArray} is invoked
     *   <li>an identifier or member select denoting a {@code private final} array field whose
     *       initializer is an empty array (e.g., {@code private final T[] EMPTY = new T[0];})
     * </ul>
     *
     * @param argument the expression passed as the {@code T[]} parameter to {@code toArray}
     * @param receiver the simple name of the {@code Collection} receiver used to match {@code new
     *     T[receiver.size()]}}
     * @return {@code true} if {@code argument} matches one of the recognized forms
     */
    private boolean isRecognizedToArrayArg(ExpressionTree argument, String receiver) {
        final NewArrayTree newArr;
        argument = TreeUtils.withoutParens(argument);

        if (argument instanceof NewArrayTree) {
            newArr = (NewArrayTree) argument;
        } else if ((argument instanceof IdentifierTree) || (argument instanceof MemberSelectTree)) {
            Element el = TreeUtils.elementFromUse(argument);
            if (el == null || !el.getKind().isField()) {
                return false;
            }
            VariableElement fieldElement = (VariableElement) el;
            if (!(fieldElement
                            .getModifiers()
                            .containsAll(EnumSet.of(Modifier.PRIVATE, Modifier.FINAL))
                    && ElementUtils.getType(fieldElement).getKind() == TypeKind.ARRAY)) {
                return false;
            }
            Tree declTree = atypeFactory.declarationFromElement(fieldElement);
            if (!(declTree instanceof VariableTree)) {
                return false;
            }
            VariableTree fieldDecl = (VariableTree) declTree;
            ExpressionTree init = fieldDecl.getInitializer();
            if (!(init instanceof NewArrayTree)) {
                return false;
            }
            newArr = (NewArrayTree) init;
        } else {
            return false;
        }

        // empty array initializer
        if (newArr.getInitializers() != null) {
            return newArr.getInitializers().isEmpty();
        }

        if (newArr.getDimensions().isEmpty()) {
            return false;
        }

        ExpressionTree dimension = newArr.getDimensions().get(newArr.getDimensions().size() - 1);
        dimension = TreeUtils.withoutParens(dimension);
        if (isLiteralZero(dimension)) {
            return true;
        }

        // size()-length array creation
        if (TreeUtils.isMethodInvocation(dimension, size, processingEnv)) {
            MethodInvocationTree invok = (MethodInvocationTree) dimension;
            return receiverName(invok.getMethodSelect()).equals(receiver);
        }

        return false;
    }

    /**
     * Returns true if the argument is a field access expression, where the field has declared type
     * {@code @ArrayLen(0)}.
     *
     * @param argument an expression tree
     * @return true if the argument is a field access expression, where the field has declared type
     *     {@code @ArrayLen(0)}
     */
    private boolean isArrayLenZeroFieldAccess(ExpressionTree argument) {
        Element el = TreeUtils.elementFromUse(argument);
        if (el != null && el.getKind().isField()) {
            TypeMirror t = ElementUtils.getType(el);
            if (t.getKind() == TypeKind.ARRAY) {
                List<? extends AnnotationMirror> ams = t.getAnnotationMirrors();
                for (AnnotationMirror am : ams) {
                    if (atypeFactory.areSameByClass(am, ArrayLen.class)) {
                        List<Integer> lens =
                                AnnotationUtils.getElementValueArray(
                                        am, arrayLenValueElement, Integer.class);
                        if (lens.size() == 1 && lens.get(0) == 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} when the supplied tree represents the integer literal {@code 0}.
     *
     * @param tree the expression to examine
     * @return {@code true} if {@code tree} is a {@link com.sun.source.tree.LiteralTree} containing
     *     integer {@code 0}
     */
    private static boolean isLiteralZero(ExpressionTree tree) {
        if (tree instanceof LiteralTree) {
            Object v = ((LiteralTree) tree).getValue();
            return v instanceof Integer && ((Integer) v) == 0;
        }
        return false;
    }

    /**
     * Returns {@code true} if the method invocation tree receiver is collection that contains
     * non-null elements (i.e. its type argument is {@code @NonNull}.
     *
     * @param tree a method invocation
     * @return true if the receiver is a collection of non-null elements
     */
    private boolean receiverIsCollectionOfNonNullElements(MethodInvocationTree tree) {
        // check receiver
        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(tree);
        AnnotatedDeclaredType collection =
                AnnotatedTypes.asSuper(atypeFactory, receiver, collectionType);

        if (collection.getTypeArguments().isEmpty() // raw type
                || !collection
                        .getTypeArguments()
                        .get(0)
                        .hasEffectiveAnnotation(atypeFactory.NONNULL)) {
            return false;
        }
        return true;
    }

    /**
     * The name of the receiver object of the tree.
     *
     * @param tree either an identifier tree or a member select tree
     * @return the name of the receiver object of the tree
     */
    // This method is quite sloppy, but works most of the time
    private String receiverName(Tree tree) {
        if (tree instanceof MemberSelectTree) {
            ExpressionTree expr = ((MemberSelectTree) tree).getExpression();
            expr = TreeUtils.withoutParens(expr);
            return expr.toString();
        } else {
            return "this";
        }
    }
}
