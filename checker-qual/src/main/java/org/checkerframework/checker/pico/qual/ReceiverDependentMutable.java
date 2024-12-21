package org.checkerframework.checker.pico.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ReceiverDependentMutable} is a type qualifier that indicates that mutability type depends
 * on the receiver type.
 *
 * <p>For usage in PICO, there are three ways to use this annotation: Object creation: the object
 * created depends on the lhs type; Annotation on a reference: the object that reference depends on
 * the receiver type; Annotation on a class: the instances can be mutable or immutable.
 */
@SubtypeOf(Readonly.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
// @DefaultFor({ TypeUseLocation.FIELD })
@HoldsForDefaultValue
public @interface ReceiverDependentMutable {}
