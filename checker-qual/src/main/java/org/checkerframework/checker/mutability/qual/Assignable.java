package org.checkerframework.checker.mutability.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field may be reassigned after initialization.
 *
 * <p>An {@code @Assignable} field is excluded from the object's abstract state for the Mutability
 * Checker's immutability checks. This annotation is only meaningful on field declarations.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@HoldsForDefaultValue
public @interface Assignable {}
