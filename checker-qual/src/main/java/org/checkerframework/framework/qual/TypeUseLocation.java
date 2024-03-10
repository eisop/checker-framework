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

    /** Apply default annotations to all unannotated raw types of fields. */
    FIELD,

    /**
     * Apply default annotations to all unannotated raw types of local variables, casts, and
     * instanceof.
     */
    LOCAL_VARIABLE,

    /** Apply default annotations to all unannotated raw types of resource variables. */
    RESOURCE_VARIABLE,

    /** Apply default annotations to all unannotated raw types of exception parameters. */
    EXCEPTION_PARAMETER,

    /** Apply default annotations to all unannotated raw types of receiver types. */
    RECEIVER,

    /**
     * Apply default annotations to all unannotated raw types of formal parameter types, excluding
     * the receiver.
     */
    PARAMETER,

    /** Apply default annotations to all unannotated raw types of return types. */
    RETURN,

    /** Apply default annotations to all unannotated raw types of constructor result types. */
    CONSTRUCTOR_RESULT,

    /**
     * Apply default annotations to unannotated lower bounds for type parameters and wildcards, both
     * explicit ones in {@code super} clauses, and implicit lower bounds when no explicit {@code
     * extends} or {@code super} clause is present.
     */
    LOWER_BOUND,

    /**
     * Apply default annotations to unannotated, but explicit lower bounds of wildcards: {@code <?
     * super C>}. Type parameters have no syntax for explicit lower bound types.
     */
    EXPLICIT_LOWER_BOUND,

    /**
     * Apply default annotations to unannotated, but implicit lower bounds for type parameters and
     * wildcards: {@code <T>} and {@code <?>}, possibly with explicit upper bounds.
     */
    // Note: no distinction between implicit lower bound when upper bound is explicit or not, in
    // contrast to what we do for upper bounds. We can add that if a type system needs it.
    IMPLICIT_LOWER_BOUND,

    /**
     * Apply default annotations to unannotated upper bounds for type parameters and wildcards: both
     * explicit ones in {@code extends} clauses, and implicit upper bounds when no explicit {@code
     * extends} or {@code super} clause is present.
     *
     * <p>Especially useful for parametrized classes that provide a lot of static methods with the
     * same generic parameters as the class.
     */
    UPPER_BOUND,

    /**
     * Apply default annotations to unannotated, but explicit type parameter and wildcard upper
     * bounds: {@code <T extends C>} and {@code <? extends C>}.
     */
    EXPLICIT_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated, but explicit type parameter upper bounds: {@code <T
     * extends C>}.
     */
    EXPLICIT_TYPE_PARAMETER_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated, but explicit wildcard upper bounds: {@code <?
     * extends C>}.
     */
    EXPLICIT_WILDCARD_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated upper bounds for type parameters and wildcards
     * without explicit upper bounds: {@code <T>}, {@code <?>}, and {@code <? super C>}.
     */
    IMPLICIT_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated upper bounds for type parameters without explicit
     * upper bounds: {@code <T>}.
     */
    IMPLICIT_TYPE_PARAMETER_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated upper bounds for wildcards without a super bound:
     * {@code <?>}.
     */
    IMPLICIT_WILDCARD_UPPER_BOUND_NO_SUPER,

    /**
     * Apply default annotations to unannotated upper bounds for wildcards with a super bound:
     * {@code <? super C>}.
     */
    IMPLICIT_WILDCARD_UPPER_BOUND_SUPER,

    /**
     * Apply default annotations to unannotated upper bounds for wildcards with or without a super
     * bound: {@code <?>} or {@code <? super C>}.
     */
    IMPLICIT_WILDCARD_UPPER_BOUND,

    /**
     * Apply default annotations to unannotated type variable uses: {@code T}.
     *
     * <p>To get parametric polymorphism: add a qualifier that is meta-annotated with {@link
     * ParametricTypeVariableUseQualifier} to your type system and use it as default for {@code
     * TYPE_VARIABLE_USE}, which is treated like no annotation on the type variable use.
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
