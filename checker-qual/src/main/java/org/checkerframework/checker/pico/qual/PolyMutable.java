package org.checkerframework.checker.pico.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A polymorphic qualifier for the PICO mutability hierarchy.
 *
 * <p>{@code @PolyMutable} links the mutability of a method's receiver, parameters, local variables,
 * and return type. At each call site, all occurrences of {@code @PolyMutable} in one polymorphic
 * use are substituted with the same concrete mutability qualifier.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(Readonly.class)
@TargetLocations({
    TypeUseLocation.PARAMETER,
    TypeUseLocation.RECEIVER,
    TypeUseLocation.RETURN,
    TypeUseLocation.LOCAL_VARIABLE
})
public @interface PolyMutable {}
