package org.checkerframework.checker.mutability.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top qualifier in the mutability hierarchy.
 *
 * <p>A reference of type {@code @Readonly T} does not permit mutation of the referenced object's
 * fields through that reference. The object might still be mutated through another alias with a
 * more specific mutability type.
 *
 * <p>This is the default qualifier for local variables and implicit upper bounds. Local variables
 * are subject to flow-sensitive refinement.
 */
@SubtypeOf({})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor({TypeUseLocation.LOCAL_VARIABLE, TypeUseLocation.IMPLICIT_UPPER_BOUND})
public @interface Readonly {}
