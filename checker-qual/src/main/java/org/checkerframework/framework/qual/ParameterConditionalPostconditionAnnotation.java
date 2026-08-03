package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation E is a <b>parameter</b> conditional
 * postcondition annotation: E is written on a formal parameter and expresses that the annotated
 * parameter has a given qualifier when the method returns a given boolean value.
 *
 * <p>E either has a single element {@code value()} of type {@code boolean} with the same meaning as
 * {@link EnsuresQualifierIf#result()}, or E has no such element and the result is fixed by this
 * meta-annotation's {@link #result()} (for concrete annotations like
 * {@code @NonNullIfReturnsTrue}). The expression to which the qualifier applies is implicit: it is
 * the annotated parameter (e.g. {@code "#1"} for the first parameter).
 *
 * <p>For example, the nullness checker defines {@link
 * org.checkerframework.checker.nullness.qual.NonNullIfReturns}:
 *
 * <pre><code>
 * {@literal @}ParameterConditionalPostconditionAnnotation(qualifier = NonNull.class)
 * {@literal @}Target(ElementType.PARAMETER)
 * public {@literal @}interface NonNullIfReturns {
 *   boolean value();
 * }
 * </code></pre>
 *
 * <p>Usage: {@code @NonNullIfReturns(true)} or {@code @NonNullIfReturnsTrue} means the parameter is
 * non-null when the method returns true; {@code @NonNullIfReturns(false)} or
 * {@code @NonNullIfReturnsFalse} when it returns false.
 *
 * @see ConditionalPostconditionAnnotation
 * @see EnsuresQualifierIf
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface ParameterConditionalPostconditionAnnotation {
    /**
     * The qualifier that is established on the annotated parameter when the method returns the
     * value specified by the parameter annotation's {@code value()} element, or by this {@link
     * #result()} when the annotation has no {@code value()} (e.g. {@code @NonNullIfReturnsTrue}).
     *
     * @return the qualifier
     */
    Class<? extends Annotation> qualifier();

    /**
     * The return value under which the qualifier holds. Used when the concrete annotation has no
     * {@code value()} element (e.g. {@code @NonNullIfReturnsTrue} /
     * {@code @NonNullIfReturnsFalse}). Ignored when the concrete annotation has {@code value()}.
     *
     * @return true if the qualifier holds when the method returns true, false when it returns false
     */
    boolean result() default true;
}
