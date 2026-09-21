package org.checkerframework.checker.testchecker.ainfer.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Toy type system for testing field inference.
 *
 * @see AinferSibling1
 * @see AinferSibling2
 * @see AinferParent
 */
@SubtypeOf({AinferImplicitAnno.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.LOWER_BOUND, TypeUseLocation.UPPER_BOUND})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface AinferBottom {}
