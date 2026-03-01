package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.ParameterConditionalPostconditionAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A <b>parameter</b> contract: expresses that the parameter is non-null when the method returns the
 * given value.
 *
 * <p><b>Semantic meaning:</b> {@code @NotNullIfReturns(v)} means: <i>this parameter is non-null
 * when the method returns v</i>. That is: {@code method returns value() ⇒ parameter != null}.
 *
 * <p>This annotation only makes sense on parameters whose root type is {@code @Nullable} or
 * parametric. The checker refines the parameter's type to {@code @NonNull} in code paths where the
 * method's return value is known (e.g., inside {@code if (method(arg))} or {@code if
 * (!method(arg))} blocks).
 *
 * <p><b>Example 1: hasLength</b> — param is non-null when method returns true.
 *
 * <pre>{@code
 * public static boolean hasLength(@NotNullIfReturns(true) @Nullable String str) {
 *   return (str != null && !str.isEmpty());
 * }
 * }</pre>
 *
 * When {@code hasLength(s)} returns true, {@code s} was non-null.
 *
 * <p><b>Example 2: equals</b> — param is non-null when method returns true.
 *
 * <pre>{@code
 * @Override
 * public boolean equals(@NotNullIfReturns(true) @Nullable Object other) {
 *   return this == other;
 * }
 * }</pre>
 *
 * <p><b>Example 3: isNull</b> — param is non-null when method returns false.
 *
 * <pre>{@code
 * public static boolean isNull(@NotNullIfReturns(false) @Nullable Object obj) {
 *   return obj == null;
 * }
 * }</pre>
 *
 * When {@code isNull(obj)} returns false, {@code obj} was non-null.
 *
 * <ul>
 *   <li><b>Scope:</b> The annotation applies only to the parameter on which it is written.
 *   <li><b>Meaning:</b> {@code @NotNullIfReturns(v)} means: param is non-null when method returns
 *       v.
 *   <li><b>Target:</b> Only formal parameters.
 * </ul>
 *
 * @see EnsuresNonNullIf
 * @see NonNull
 * @see Nullable
 * @see org.checkerframework.checker.nullness.NullnessChecker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@ParameterConditionalPostconditionAnnotation(qualifier = NonNull.class)
public @interface NotNullIfReturns {
    /**
     * The return value under which the parameter is non-null.
     *
     * @return the return value under which the parameter has the qualifier (e.g., {@code true} for
     *     {@code hasLength}, {@code false} for {@code isNull})
     */
    boolean value();
}
