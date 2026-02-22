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
 * <p>This is the parameter-level analogue of {@link ConditionalPostconditionAnnotation}. It allows
 * expressing conditional postconditions on parameters without putting the contract on the method
 * and without using expression strings like {@code "#1"}.
 *
 * <p>For example, the nullness checker defines:
 *
 * <pre><code>
 * {@literal @}ParameterConditionalPostconditionAnnotation(qualifier = NonNull.class)
 * {@literal @}Target(ElementType.PARAMETER)
 * public {@literal @}interface NonNullIfReturn {
 *   boolean value();
 * }
 * </code></pre>
 *
 * <p>Usage:
 *
 * <pre><code>
 * public boolean equals(
 *     {@literal @}NonNullIfReturn(true) {@literal @}Nullable Object other
 * ) { ... }
 * </code></pre>
 *
 * <p>This is equivalent to the method-level contract {@code @EnsuresNonNullIf(expression="#1",
 * result=true)} on the method.
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
