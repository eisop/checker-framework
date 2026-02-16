package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.ParameterConditionalPostconditionAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A <b>parameter</b> contract: indicates that the annotated parameter is guaranteed to be non-null
 * when the method returns the given boolean value.
 *
 * <p>This is the parameter-level analogue of {@link EnsuresNonNullIf}. It expresses the same
 * conditional postcondition (this parameter is non-null if the method returns {@code value()}) but
 * is written on the parameter instead of the method, so no expression string like {@code "#1"} is
 * needed.
 *
 * <p><b>Example 1: Simple null check</b>
 *
 * <pre>{@code
 * public static boolean isNonNull(
 *     @NonNullIfReturn(true) @Nullable Object obj
 * ) {
 *   return obj != null;
 * }
 * }</pre>
 *
 * If {@code isNonNull(obj)} returns {@code true}, then {@code obj} is non-null after the call.
 * Equivalent to the method-level contract {@code @EnsuresNonNullIf(expression="#1", result=true)}.
 *
 * <p><b>Example 2: equals(Object)</b>
 *
 * <pre>{@code
 * @Override
 * public boolean equals(
 *     @NonNullIfReturn(true) @Nullable Object other
 * ) {
 *   ...
 * }
 * }</pre>
 *
 * If {@code equals(other)} returns {@code true}, then {@code other} was non-null.
 *
 * <p><b>Example 3: Negative return value</b>
 *
 * <pre>{@code
 * public static boolean isNull(
 *     @NonNullIfReturn(false) @Nullable Object obj
 * ) {
 *   return obj == null;
 * }
 * }</pre>
 *
 * If {@code isNull(obj)} returns {@code false}, then {@code obj} is non-null.
 *
 * <p><b>Example 4: Multiple parameters</b>
 *
 * <pre>{@code
 * public static boolean bothNonNull(
 *     @NonNullIfReturn(true) @Nullable Object a,
 *     @NonNullIfReturn(true) @Nullable Object b
 * ) {
 *   return a != null && b != null;
 * }
 * }</pre>
 *
 * If the method returns {@code true}, then {@code a} is non-null and {@code b} is non-null. Each
 * parameter's annotation contributes independently.
 *
 * <ul>
 *   <li><b>Scope:</b> The annotation applies only to the parameter on which it is written. The
 *       "expression" of the contract is implicitly that parameter (e.g. the first parameter is
 *       {@code "#1"}).
 *   <li><b>Meaning:</b> For a call {@code m(...)} that returns {@code v}, if {@code v} equals the
 *       annotation's {@code value()}, then the Nullness Checker treats the corresponding argument
 *       as {@link NonNull} at program points after the call (in the same way as for {@link
 *       EnsuresNonNullIf} with {@code expression="#n"} and {@code result=value()}).
 *   <li><b>One-way guarantee:</b> No converse implication is assumed. For example,
 *       {@code @NonNullIfReturn(true)} does not imply that when the method returns {@code false},
 *       the parameter is null.
 *   <li><b>Target:</b> Only formal parameters. Putting this annotation on a method or field is a
 *       misuse and may be rejected by the checker or ignored.
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
public @interface NonNullIfReturn {
    /**
     * The return value of the method under which the annotated parameter is guaranteed to be
     * non-null.
     *
     * @return the return value under which the parameter is non-null (typically {@code true} for
     *     methods like {@code equals} or {@code isNonNull}, or {@code false} for methods like
     *     {@code isNull})
     */
    boolean value();
}
