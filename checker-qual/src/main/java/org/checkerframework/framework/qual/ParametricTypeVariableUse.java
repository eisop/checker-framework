package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Type qualifier to mark that a type variable use is parametric. This is only a temporary way to
 * allow setting a default for a {@link TypeUseLocation#TYPE_VARIABLE_USE} that expresses that the
 * type variable use should remain parametric.
 *
 * <p>In a separate PR I will implement this more nicely using a meta-annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface ParametricTypeVariableUse {}
