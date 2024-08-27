package org.checkerframework.framework.qual;

/**
 * Specifies the locations to which a {@link DefaultQualifier} annotation applies.
 *
 * <p>The order of enums is important. Defaults are applied in this order. In particular, this means
 * that OTHERWISE and ALL should be last.
 *
 * <p>For an annotation on a variable which has element kind ENUM_CONSTANT, the annotation's
 * type-use location is either {@code TypeUseLocation.FIELD} or {@code
 * TypeUseLocation.CONSTRUCTOR_RESULT}.
 *
 * <p>Note: The use locations listed here are not complete, for more details see <a href=
 * "https://github.com/eisop/checker-framework/issues/340">EISOP Issue #340</a>
 *
 * @see DefaultQualifier
 * @see javax.lang.model.element.ElementKind
 */
public enum TypeUseLocation {

    /** Apply default annotations to unannotated top-level types of fields. */
    FIELD,

    /**
     * Apply default annotations to unannotated top-level types of local variables, casts, and
     * instanceof.
     */
    LOCAL_VARIABLE,

    /** Apply default annotations to unannotated top-level types of resource variables. */
    RESOURCE_VARIABLE,

    /** Apply default annotations to unannotated top-level types of exception parameters. */
    EXCEPTION_PARAMETER,

    /** Apply default annotations to unannotated top-level types of receiver types. */
    RECEIVER,

    /**
     * Apply default annotations to unannotated top-level types of formal parameter types, excluding
     * the receiver.
     */
    PARAMETER,

    /** Apply default annotations to unannotated top-level types of return types. */
    RETURN,

    /** Apply default annotations to unannotated top-level types of constructor result types. */
    CONSTRUCTOR_RESULT,

    /**
     * Apply default annotations to unannotated top-level lower bounds of type parameters and
     * wildcards, both explicit ones in {@code super} clauses, and implicit lower bounds when no
     * explicit {@code extends} or {@code super} clause is present.
     */
    LOWER_BOUND,

    /**
     * Apply default annotations to unannotated top-level explicit lower bounds of wildcards: {@code
     * <? super C>}. Type parameters have no syntax for explicit lower bound types.
     */
    EXPLICIT_LOWER_BOUND,

    /**
     * Apply default annotations to unannotated implicit lower bounds of type parameters and
     * wildcards: {@code <T>} and {@code <?>}, possibly with explicit upper bounds.
     */
    // Note: no distinction between implicit lower bound when upper bound is explicit or not, in
    // contrast to what we do for upper bounds. We can add that if a type system needs it.
    IMPLICIT_LOWER_BOUND,

    /**
     * Apply default annotations to unannotated top-level upper bounds of type parameters and
     * wildcards: both explicit ones in {@code extends} clauses, and implicit upper bounds when no
     * explicit {@code extends} or {@code super} clause is present.
     *
     * <p>Especially useful for parametrized classes that provide a lot of static methods with the
     * same generic parameters as the class.
     */
    UPPER_BOUND,

    /**
     * Apply default annotations to unannotated top-level explicit type parameter and wildcard upper
     * bounds: {@code <T extends C>} and {@code <? extends C>}.
     */
    EXPLICIT_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated top-level explicit type parameter upper bounds:
     * {@code <T extends C>}.
     */
    EXPLICIT_TYPE_PARAMETER_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated top-level explicit wildcard upper bounds: {@code <?
     * extends C>}.
     */
    EXPLICIT_WILDCARD_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated upper bounds of type parameters and wildcards
     * without explicit upper bounds: {@code <T>}, {@code <?>}, and {@code <? super C>}.
     */
    IMPLICIT_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated upper bounds of type parameters without explicit
     * upper bounds: {@code <T>}.
     */
    IMPLICIT_TYPE_PARAMETER_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated upper bounds of wildcards without a super bound:
     * {@code <?>}.
     */
    IMPLICIT_WILDCARD_UPPER_BOUND_NO_SUPER,

    /**
     * Apply default annotations to unannotated upper bounds of wildcards with a super bound: {@code
     * <? super C>}.
     */
    IMPLICIT_WILDCARD_UPPER_BOUND_SUPER,

    /**
     * Apply default annotations to unannotated upper bounds of wildcards with or without a super
     * bound: {@code <?>} or {@code <? super C>}.
     */
    IMPLICIT_WILDCARD_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated type variable uses that are not top-level local
     * variables: {@code T field} or {@code List<T> local}.
     *
     * <p>Such uses of type variables are not flow-sensitively refined and are therefore usually
     * parametric.
     *
     * <p>To get parametric polymorphism: add a qualifier that is meta-annotated with {@link
     * ParametricTypeVariableUseQualifier} to your type system and use it as default for {@code
     * TYPE_VARIABLE_USE}, which is treated like no annotation on the type variable use.
     *
     * <p>We could name this constant {@code TYPE_VARIABLE_USE_NOT_TOP_LEVEL_LOCAL_VARIABLE} and
     * introduce a separate constant {@code TYPE_VARIABLE_USE_TOP_LEVEL_LOCAL_VARIABLE}. At the
     * moment we use the {@code LOCAL_VARIABLE} default for unannotated top-level type variable uses
     * of local variables: {@code T local}.
     */
    TYPE_VARIABLE_USE,

    /** Apply if nothing more concrete is provided. TODO: clarify relation to ALL. */
    OTHERWISE,

    /**
     * Apply default annotations to all type uses other than uses of type parameters. Does not allow
     * any of the other constants. Usually you want OTHERWISE.
     */
    ALL;
}
