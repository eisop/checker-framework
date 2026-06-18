package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.ParameterConditionalPostconditionAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A <b>parameter</b> contract: the parameter is non-null when the method returns {@code true}.
 *
 * <p>Equivalent to {@code @NonNullIfReturns(true)} but with a fixed value for use when the return
 * condition is always true (e.g. {@code hasLength}, {@code equals}).
 *
 * @see NonNullIfReturns
 * @see NonNullIfReturnsFalse
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@ParameterConditionalPostconditionAnnotation(qualifier = NonNull.class, result = true)
public @interface NonNullIfReturnsTrue {}
