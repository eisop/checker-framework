package org.checkerframework.checker.pico.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Mutable} is a type qualifier that indicates that the fields of annotated reference can be
 * mutated through this reference. This is default behavior for all references in Java.
 *
 * <p>For usage in PICO, there are three ways to use this annotation: Object creation: the object
 * created will always be mutable; Annotation on a reference: the object that reference points to is
 * mutable; Annotation on a class: all instances of that class are mutable.
 */
@SubtypeOf({Readonly.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
// @DefaultFor({ TypeUseLocation.EXCEPTION_PARAMETER })
@HoldsForDefaultValue
public @interface Mutable {}
