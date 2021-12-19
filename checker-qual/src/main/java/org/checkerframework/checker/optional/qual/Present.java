package org.checkerframework.checker.optional.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link java.util.Optional Optional} container definitely contains a (non-null) value.
 *
 * @checker_framework.manual #optional-checker Optional Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({MaybePresent.class})
public @interface Present {}
