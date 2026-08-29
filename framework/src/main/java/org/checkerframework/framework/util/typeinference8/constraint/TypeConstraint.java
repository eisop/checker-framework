package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.UseOfVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.javacutil.SwitchExpressionScanner;
import org.checkerframework.javacutil.SwitchExpressionScanner.FunctionalSwitchExpressionScanner;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import javax.lang.model.type.TypeKind;

/**
 * Constraints are between either an expression and a type, two types, or an expression and a thrown
 * type. Defined in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.1.2">JLS section
 * 18.1.2</a>
 */
public abstract class TypeConstraint implements Constraint {

    /** T, the type on the right hand side of the constraint; may contain inference variables. */
    protected AbstractType T;

    /**
     * The constraint whose reduction created this constraint or null if this constraint isn't from
     * a reduction from another. If null, then {@code source} should be nonnull.
     */
    public @Nullable Constraint parent;

    /**
     * A string that describes where this constraint is from. If null, then the constraint came from
     * reducing {@code parent}.
     */
    public @Nullable String source;

    /**
     * Creates a type constraint
     *
     * @param source a string describing where this constraint came from
     * @param T the type of the right hand side of the constraint
     */
    protected TypeConstraint(String source, AbstractType T) {
        assert T != null : "Can't create a constraint with a null type.";
        this.T = T;
        this.parent = null;
        this.source = source;
    }

    /**
     * Creates a type constraint
     *
     * @param parent the constraint whose reduction created this constraint
     * @param T the type of the right hand side of the constraint
     */
    protected TypeConstraint(Constraint parent, AbstractType T) {
        assert T != null : "Can't create a constraint with a null type.";
        this.T = T;
        this.parent = parent;
        this.source = null;
    }

    /**
     * Returns a string that explains where this constraint came from.
     *
     * @return a string that explains where this constraint came from
     */
    public String constraintHistory() {
        StringJoiner constraintStack = new StringJoiner(System.lineSeparator());
        constraintStack.add(this.toString());

        Constraint parent = this.parent;
        String source = this.source;
        while (parent != null) {
            constraintStack.add((source != null ? source + ": " : "") + parent);

            if (parent instanceof TypeConstraint) {
                source = ((TypeConstraint) parent).source;
                parent = ((TypeConstraint) parent).parent;
            } else {
                parent = null;
            }
        }
        if (source != null) {
            constraintStack.add("From: " + source);
        }
        return constraintStack.toString();
    }

    /**
     * Returns T which is the type on the right hand side of the constraint.
     *
     * @return T, that is the type on the right hand side of the constraint
     */
    public AbstractType getT() {
        return T;
    }

    /**
     * Returns a set of all inference variables mentioned by this constraint.
     *
     * <p>The mutability/freshness guarantees are the same as {@link
     * AbstractType#getInferenceVariables()}.
     *
     * @return a set of all inference variables mentioned by this constraint
     */
    public Set<Variable> getInferenceVariables() {
        return T.getInferenceVariables();
    }

    /**
     * For lambda and method references constraints, input variables are roughly the inference
     * variables mentioned by they function type's parameter types and return types. For conditional
     * expression constraints and switch expression constraints, input variables are the union of
     * the input variables of its subexpressions. For all other constraints, no input variables
     * exist.
     *
     * <p>Defined in <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
     * section 18.5.2.2</a>
     *
     * <p>Callers that need to mutate the result should make a defensive copy first.
     *
     * @return input variables for this constraint
     */
    public abstract Set<Variable> getInputVariables();

    /**
     * "The output variables of [expression] constraints are all inference variables mentioned by
     * the type on the right-hand side of the constraint, T, that are not input variables."
     *
     * <p>As defined in <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
     * section 18.5.2.2</a>
     *
     * <p>Callers that need to mutate the result should make a defensive copy first.
     *
     * @return output variables for this constraint
     */
    public abstract Set<Variable> getOutputVariables();

    /**
     * Returns the union of {@code inputs} and {@code toAdd} without modifying {@code inputs}.
     *
     * <p>This method avoids allocation when {@code toAdd} is empty.
     *
     * @param inputs the input set
     * @param toAdd the set to add
     * @return {@code inputs} itself when {@code toAdd} is empty; otherwise a new mutable set
     *     containing all variables from {@code inputs} and {@code toAdd}
     */
    protected static Set<Variable> addAllLazily(Set<Variable> inputs, Set<Variable> toAdd) {
        if (toAdd.isEmpty()) {
            return inputs;
        }
        if (inputs.isEmpty()) {
            return new LinkedHashSet<>(toAdd);
        }
        Set<Variable> result = new LinkedHashSet<>(inputs);
        result.addAll(toAdd);
        return result;
    }

    /**
     * Implementation of {@link #getInputVariables()} that is used both by expressions constraints
     * and checked exception constraints
     * https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.5.2-200
     *
     * @param tree an expression tree
     * @param T the type of the right hand side of the constraint
     * @return the input variables for this constraint
     */
    protected Set<Variable> getInputVariablesForExpression(ExpressionTree tree, AbstractType T) {
        switch (tree.getKind()) {
            case LAMBDA_EXPRESSION:
                if (T.isUseOfVariable()) {
                    return Collections.singleton(((UseOfVariable) T).getVariable());
                } else {
                    LambdaExpressionTree lambdaTree = (LambdaExpressionTree) tree;
                    Set<Variable> inputs = Collections.emptySet();
                    if (TreeUtils.isImplicitlyTypedLambda(lambdaTree)) {
                        List<AbstractType> params = this.T.getFunctionTypeParameterTypes();
                        if (params == null) {
                            // T is not a function type.
                            return Collections.emptySet();
                        }
                        for (AbstractType param : params) {
                            inputs = addAllLazily(inputs, param.getInferenceVariables());
                        }
                    }
                    AbstractType R = this.T.getFunctionTypeReturnType();
                    if (R == null || R.getTypeKind() == TypeKind.NONE) {
                        return inputs;
                    }
                    for (ExpressionTree e : TreeUtils.getReturnedExpressions(lambdaTree)) {
                        TypeConstraint c = new Expression("Returned expression constraint", e, R);
                        inputs = addAllLazily(inputs, c.getInputVariables());
                    }
                    return inputs;
                }
            case MEMBER_REFERENCE:
                if (T.isUseOfVariable()) {
                    return Collections.singleton(((UseOfVariable) T).getVariable());
                } else if (TreeUtils.isExactMethodReference((MemberReferenceTree) tree)) {
                    return Collections.emptySet();
                } else {
                    List<AbstractType> params = this.T.getFunctionTypeParameterTypes();
                    if (params == null) {
                        // T is not a function type.
                        return Collections.emptySet();
                    }
                    Set<Variable> inputs = Collections.emptySet();
                    for (AbstractType param : params) {
                        inputs = addAllLazily(inputs, param.getInferenceVariables());
                    }
                    return inputs;
                }
            case PARENTHESIZED:
                return getInputVariablesForExpression(TreeUtils.withoutParens(tree), T);
            case CONDITIONAL_EXPRESSION:
                ConditionalExpressionTree conditional = (ConditionalExpressionTree) tree;
                Set<Variable> inputs = Collections.emptySet();
                inputs =
                        addAllLazily(
                                inputs,
                                getInputVariablesForExpression(conditional.getTrueExpression(), T));
                inputs =
                        addAllLazily(
                                inputs,
                                getInputVariablesForExpression(
                                        conditional.getFalseExpression(), T));
                return inputs;
            default:
                if (TreeUtils.isSwitchExpression(tree)) {
                    Set<Variable> inputs2 = new LinkedHashSet<>();

                    SwitchExpressionScanner<Boolean, Void> scanner =
                            new FunctionalSwitchExpressionScanner<>(
                                    (ExpressionTree exTree, Void unused) ->
                                            inputs2.addAll(
                                                    getInputVariablesForExpression(exTree, T)),
                                    (r1, r2) -> null);
                    scanner.scanSwitchExpression(tree, null);
                    return inputs2;
                }
                return Collections.emptySet();
        }
    }

    /**
     * Apply the given instantiations to any type mentioned in this constraint -- meaning replace
     * any mention of a variable in {@code instantiations} with its proper type.
     */
    public void applyInstantiations() {
        T = T.applyInstantiations();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TypeConstraint that = (TypeConstraint) o;

        return T.equals(that.T);
    }

    /** Cached hash code to prevent repeated recomputation of complex deep-hashes. */
    private int cachedHashCode = 0;

    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
            int result = T.hashCode();
            cachedHashCode = result == 0 ? 1 : result;
        }
        return cachedHashCode;
    }
}
