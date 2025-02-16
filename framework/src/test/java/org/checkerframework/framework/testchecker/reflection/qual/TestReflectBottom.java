package org.checkerframework.framework.testchecker.reflection.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * Toy type system for testing reflection resolution. Uses
 * org.checkerframework.common.subtyping.qual.Bottom as bottom.
 *
 * @see TestReflectSibling1
 * @see TestReflectSibling2
 */
@SubtypeOf({TestReflectSibling1.class, TestReflectSibling2.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.LOWER_BOUND, TypeUseLocation.UPPER_BOUND})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface TestReflectBottom {}
