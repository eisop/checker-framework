package org.checkerframework.framework.util.typeinference8.constraint;

import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

import java.util.Set;

/**
 * A constraint. See <a
 * href="https://docs.oracle.com/javase/specs/jls/se20/html/jls-18.html#jls-18.1.2">JLS</a>.
 */
public interface Constraint extends ReductionResult {

    /**
     * Return the kind of constraint.
     *
     * @return the kind of constraint
     */
    Kind getKind();

    /**
     * Reduce this constraint what this means depends on the kind of constraint. Reduction can
     * produce new bounds and/or new constraints.
     *
     * <p>Reduction is documented in <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.2">JLS section
     * 18.2</a>
     *
     * @param context Java8InferenceContext
     * @return the result of reducing this constraint
     */
    ReductionResult reduce(Java8InferenceContext context);

    /**
     * Returns the input variables of this constraint: the inference variables that must be resolved
     * before this constraint may be reduced.
     *
     * <p>For lambda, method reference, and additional-argument constraints, input variables are
     * roughly the inference variables mentioned by the target function type's parameter types. For
     * conditional expression constraints and switch expression constraints, input variables are the
     * union of the input variables of their subexpressions. For all other constraints, no input
     * variables exist.
     *
     * <p>Defined in <a
     * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.5.2.2">JLS
     * section 18.5.2.2</a>. ({@link Kind#ADDITIONAL_ARG} constraints are this implementation's own
     * addition, not a JLS-defined constraint kind, but they need the same ordering treatment: see
     * {@link AdditionalArgument}.)
     *
     * <p>Callers that need to mutate the result should make a defensive copy first.
     *
     * @return the input variables of this constraint
     */
    Set<Variable> getInputVariables();

    /** A kind of Constraint. */
    enum Kind {
        /**
         * {@code < Expression -> T >}: An expression is compatible in a loose invocation context
         * with type T
         */
        EXPRESSION,
        /** {@code < S -> T >}: A type S is compatible in a loose invocation context with type T */
        TYPE_COMPATIBILITY,
        /** {@code < S <: T >}: A reference type S is a subtype of a reference type T */
        SUBTYPE,
        /** {@code < S <= T >}: A type argument S is contained by a type argument T. */
        CONTAINED,
        /**
         * {@code < S = T >}: A type S is the same as a type T, or a type argument S is the same as
         * type argument T.
         */
        TYPE_EQUALITY,
        /**
         * {@code < LambdaExpression -> throws T>}: The checked exceptions thrown by the body of the
         * LambdaExpression are declared by the throws clause of the function type derived from T.
         */
        LAMBDA_EXCEPTION,
        /**
         * {@code < MethodReferenceExpression -> throws T>}: The checked exceptions thrown by the
         * referenced method are declared by the throws clause of the function type derived from T.
         */
        METHOD_REF_EXCEPTION,

        /** {@code < Q <: R >}: A qualifier Q is a subtype of a qualifier R. */
        QUALIFIER_SUBTYPE,

        /** {@code < Q = R >}: A qualifier R is the same as a qualifier R. */
        QUALIFIER_EQUALITY,

        /** A single constraint, that when reduced, generates additional argument constraints. */
        ADDITIONAL_ARG,
    }
}
