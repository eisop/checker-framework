package org.checkerframework.checker.pico.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the effective mutability is determined by viewpoint adaptation from the receiver.
 *
 * <p>On a member type, {@code @ReceiverDependentMutable} is a placeholder that is replaced by the
 * mutability of the receiver at a use site. On an object creation expression, the created object's
 * mutability is determined by the target type. On a class declaration, the class may be used at
 * different mutability instantiations, such as {@code @Mutable} or {@code @Immutable}.
 *
 * <p>This qualifier is only meaningful where there is a receiver or target context from which PICO
 * can determine a concrete mutability.
 */
@SubtypeOf(Readonly.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@HoldsForDefaultValue
public @interface ReceiverDependentMutable {}
