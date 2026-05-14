package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;

import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.UseOfVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TreeUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.type.TypeKind;

/**
 * &lt;LambdaExpression &rarr;throws T&gt;: The checked exceptions thrown by the body of the
 * LambdaExpression are declared by the throws clause of the function type derived from T.
 *
 * <p>&lt;MethodReference &rarr;throws T&gt;: The checked exceptions thrown by the referenced method
 * are declared by the throws clause of the function type derived from T.
 */
public class CheckedExceptionConstraint extends TypeConstraint {

    /**
     * {@link com.sun.source.tree.LambdaExpressionTree} or {@link
     * com.sun.source.tree.MemberReferenceTree} for this constraint.
     */
    protected final ExpressionTree expression;

    /** The mapping from type variable to inference variable to use with this constraint. */
    protected final Theta map;

    /**
     * Creates a {@code CheckedExceptionConstraint}.
     *
     * @param expression {@link com.sun.source.tree.LambdaExpressionTree} or {@link
     *     com.sun.source.tree.MemberReferenceTree} for this constraint
     * @param t a function type
     * @param map The mapping from type variable to inference variable to use with this constraint
     */
    public CheckedExceptionConstraint(ExpressionTree expression, AbstractType t, Theta map) {
        super("Checked exception for " + expression, t);
        assert expression instanceof LambdaExpressionTree
                || expression instanceof MemberReferenceTree;
        this.expression = expression;
        this.map = map;
    }

    @Override
    public Kind getKind() {
        return expression instanceof LambdaExpressionTree
                ? Kind.LAMBDA_EXCEPTION
                : Kind.METHOD_REF_EXCEPTION;
    }

    @Override
    public Set<Variable> getInputVariables() {
        if (getKind() == Kind.LAMBDA_EXCEPTION) {
            if (T.isUseOfVariable()) {
                return Collections.singleton(((UseOfVariable) T).getVariable());
            } else {
                LambdaExpressionTree lambdaTree = (LambdaExpressionTree) expression;
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
                inputs = addAllLazily(inputs, R.getInferenceVariables());
                return inputs;
            }
        } else if (getKind() == Kind.METHOD_REF_EXCEPTION) {
            if (T.isUseOfVariable()) {
                return Collections.singleton(((UseOfVariable) T).getVariable());
            } else if (TreeUtils.isExactMethodReference((MemberReferenceTree) expression)) {
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
                AbstractType R = this.T.getFunctionTypeReturnType();
                if (R == null || R.getTypeKind() == TypeKind.NONE) {
                    return inputs;
                }
                inputs = addAllLazily(inputs, R.getInferenceVariables());
                return inputs;
            }
        }
        return getInputVariablesForExpression(expression, getT());
    }

    /**
     * Lazily adds all variables in {@code toAdd} to {@code inputs}.
     *
     * <p>If {@code inputs} is empty, this method avoids allocation until a non-empty set needs to
     * be added.
     */
    private static Set<Variable> addAllLazily(Set<Variable> inputs, Set<Variable> toAdd) {
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

    @Override
    public Set<Variable> getOutputVariables() {
        Set<Variable> input = getInputVariables();
        Set<Variable> output = new LinkedHashSet<>(getT().getInferenceVariables());
        output.removeAll(input);
        return output;
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
        // See JLS 18.2.5
        return context.inferenceTypeFactory.getCheckedExceptionConstraints(expression, T, map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CheckedExceptionConstraint that = (CheckedExceptionConstraint) o;

        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (expression != null ? expression.hashCode() : 0);
        return result;
    }
}
