package org.checkerframework.checker.mutability.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a reference permits mutation of the referenced object's fields.
 *
 * <p>When written on an object creation expression, the newly-created object is mutable. When
 * written on a reference type, the object may be mutated through that reference. When written on a
 * class declaration, instances of that class have a mutable declaration bound.
 *
 * <p>{@code @Mutable} is the default qualifier in the mutability hierarchy.
 */
@SubtypeOf({Readonly.class})
@Documented
@DefaultQualifierInHierarchy
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(TypeUseLocation.EXCEPTION_PARAMETER)
@HoldsForDefaultValue
public @interface Mutable {}
