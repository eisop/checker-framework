package org.checkerframework.checker.mutability.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Internal qualifier used when viewpoint adaptation loses precise mutability information.
 *
 * <p>{@code @MutabilityLost} is a subtype of {@link Readonly}. Programmers should generally write a
 * concrete mutability qualifier instead.
 */
@SubtypeOf({Readonly.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface MutabilityLost {}
