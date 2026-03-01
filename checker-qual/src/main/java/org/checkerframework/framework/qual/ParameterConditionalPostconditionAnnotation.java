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
 * <p>E must have a single element {@code value()} of type {@code boolean} with the same meaning as
 * {@link EnsuresQualifierIf#result()}. The expression to which the qualifier applies is implicit:
 * it is the annotated parameter (e.g. {@code "#1"} for the first parameter).
 *
 * <p>For example, the nullness checker defines {@link
 * org.checkerframework.checker.nullness.qual.NotNullIfReturns}:
 *
 * <pre><code>
 * {@literal @}ParameterConditionalPostconditionAnnotation(qualifier = NonNull.class)
 * {@literal @}Target(ElementType.PARAMETER)
 * public {@literal @}interface NotNullIfReturns {
 *   boolean value();
 * }
 * </code></pre>
 *
 * <p>Usage: {@code @NotNullIfReturns(true)} means the parameter is non-null when the method returns
 * true.
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
     * value specified by the parameter annotation's {@code value()} element.
     *
     * @return the qualifier
     */
    Class<? extends Annotation> qualifier();
}
