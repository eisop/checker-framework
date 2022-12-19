package org.checkerframework.framework.testchecker.reflection.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Toy type system for testing reflection resolution. Uses
 * org.checkerframework.common.subtyping.qual.Bottom as bottom.
 *
 * @see Sibling1, Sibling2
 */
@SubtypeOf({Sibling1.class, Sibling2.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({
    TypeUseLocation.LOWER_BOUND,
    TypeUseLocation.UPPER_BOUND,
    TypeUseLocation.PARAMETER,
    TypeUseLocation.FIELD,
    TypeUseLocation.LOCAL_VARIABLE,
    TypeUseLocation.RETURN
})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface ReflectBottom {}
