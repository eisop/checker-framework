package org.checkerframework.framework.util.typeinference8.constraint;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree.Kind;

import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A constraint the represent additional argument constraints generated from a method or constructor
 * invocation that is a part of a larger inference problem. When this constraint is reduced it will
 * generate more constraints from the invocaton. This is because created the constraints might use
 * the type of an implicit lambda parameter for which the larger inference problem has not yet found
 * a type. So, the additional constraints can be created until after the implicit lambda parameter
 * has a type.
 *
 * <p>If the invocation is in the body of a lambda, then the input variables of this constraint are
 * those of the {@link Expression} constraint of that lambda. Like the input variables of an {@link
 * Expression} constraint, they are resolved before this constraint is reduced (see <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
 * 18.5.2.2</a>), so the lambda constraint, which is preferred to this constraint when both can be
 * reduced, has been reduced first: the parameters of the lambda have their types, and the
 * invocation has been reduced as part of this inference problem if it is part of it at all. The
 * reduction of this constraint is empty if it is not.
 */
public class AdditionalArgument implements Constraint {

    /** The tree for the method or constructor invocation for this constraint. */
    private ExpressionTree methodOrConstructorInvocation;

    /**
     * The inference variables that must be resolved before this constraint is reduced. Some of them
     * may have been resolved since this constraint was created.
     */
    private final Set<Variable> inputVariables;

    /**
     * Creates a new constraint that has no input variables.
     *
     * @param methodOrConstructorInvocation tree for the method or constructor invocation for this
     *     constraint
     */
    public AdditionalArgument(ExpressionTree methodOrConstructorInvocation) {
        this(methodOrConstructorInvocation, Collections.emptySet());
    }

    /**
     * Creates a new constraint.
     *
     * @param methodOrConstructorInvocation tree for the method or constructor invocation for this
     *     constraint
     * @param inputVariables the inference variables that must be resolved before this constraint is
     *     reduced; the caller must not modify the set afterwards
     */
    public AdditionalArgument(
            ExpressionTree methodOrConstructorInvocation, Set<Variable> inputVariables) {
        this.methodOrConstructorInvocation = methodOrConstructorInvocation;
        this.inputVariables = inputVariables;
    }

    /**
     * Returns the input variables of this constraint that have not yet been resolved. They must be
     * resolved before this constraint is reduced.
     *
     * @return the input variables of this constraint that have not yet been resolved
     */
    @Override
    public Set<Variable> getInputVariables() {
        if (inputVariables.isEmpty()) {
            return inputVariables;
        }
        Set<Variable> unresolved = new LinkedHashSet<>();
        for (Variable v : inputVariables) {
            if (v.getInstantiation() == null) {
                unresolved.add(v);
            }
        }
        return unresolved;
    }

    @Override
    public Kind getKind() {
        return Kind.ADDITIONAL_ARG;
    }

    /**
     * Returns true if the invocation of this constraint has been reduced as part of this inference
     * problem, so that its additional argument constraints can be created.
     *
     * @param context the context
     * @return true if the invocation of this constraint has been reduced as part of this inference
     *     problem
     */
    public boolean isInvocationReduced(Java8InferenceContext context) {
        return context.maps.containsKey(methodOrConstructorInvocation);
    }

    @Override
    public ConstraintSet reduce(Java8InferenceContext context) {
        if (!isInvocationReduced(context)) {
            // The invocation is not part of this problem: its constraint had a proper target type,
            // so it is inferred separately (JLS 18.2.1). Its constraints would mention variables
            // that nothing here bounds, which then resolve to their declared bounds (eisop#2337).
            return new ConstraintSet();
        }
        return reduceInvocation(context);
    }

    /**
     * Returns the additional argument constraints of the invocation, whether or not it has been
     * reduced as part of this inference problem yet. Use it only for an invocation that is part of
     * the problem.
     *
     * @param context the context
     * @return the additional argument constraints of the invocation
     */
    public ConstraintSet reduceInvocation(Java8InferenceContext context) {
        if (methodOrConstructorInvocation instanceof MethodInvocationTree) {
            MethodInvocationTree methodInvocation =
                    (MethodInvocationTree) methodOrConstructorInvocation;
            InvocationType methodType =
                    context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(methodInvocation);
            Theta newMap =
                    context.inferenceTypeFactory.createThetaForInvocation(
                            methodInvocation, methodType, context);
            ConstraintSet set =
                    context.inference.createC(methodType, methodInvocation.getArguments(), newMap);
            set.applyInstantiations();
            return set;
        } else {
            NewClassTree newClassTree = (NewClassTree) methodOrConstructorInvocation;
            InvocationType methodType =
                    context.inferenceTypeFactory.getTypeOfMethodAdaptedToUse(newClassTree);

            Theta newMap =
                    context.inferenceTypeFactory.createThetaForInvocation(
                            newClassTree, methodType, context);
            ConstraintSet set =
                    context.inference.createC(methodType, newClassTree.getArguments(), newMap);
            set.applyInstantiations();
            return set;
        }
    }
}
