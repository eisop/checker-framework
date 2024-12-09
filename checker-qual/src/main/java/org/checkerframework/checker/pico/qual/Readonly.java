package org.checkerframework.checker.pico.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top qualifier in the mutability type hierarchy that indicates that the fields of annotated
 * reference cannot be mutated through this reference but can be mutated through other Aliasing.
 * This is the default qualifier for local variables and subject to flow-sensitive type refinement.
 */
@SubtypeOf({})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor({TypeUseLocation.LOCAL_VARIABLE, TypeUseLocation.IMPLICIT_UPPER_BOUND})
public @interface Readonly {}
