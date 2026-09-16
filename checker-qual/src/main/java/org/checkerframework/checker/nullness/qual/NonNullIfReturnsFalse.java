package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.ParameterConditionalPostconditionAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A <b>parameter</b> contract: the parameter is non-null when the method returns {@code false}.
 *
 * <p>Equivalent to {@code @NonNullIfReturns(false)} but with a fixed value for use when the return
 * condition is always false (e.g. {@code isNull}).
 *
 * @see NonNullIfReturns
 * @see NonNullIfReturnsTrue
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@ParameterConditionalPostconditionAnnotation(qualifier = NonNull.class, result = false)
public @interface NonNullIfReturnsFalse {}
