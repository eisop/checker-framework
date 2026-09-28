package org.checkerframework.framework.testchecker.striplocation.quals;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A middle qualifier of the striplocation test type system whose {@link TargetLocations} permit it
 * only on upper bounds. Writing it on a lower bound is a location error; because it is above {@link
 * StripBottom} in the hierarchy, doing so also makes the bounds incompatible, which produces a
 * {@code bound.type.incompatible} cascade unless the qualifier is stripped.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({
    TypeUseLocation.UPPER_BOUND,
    TypeUseLocation.EXPLICIT_UPPER_BOUND,
    TypeUseLocation.IMPLICIT_UPPER_BOUND
})
@SubtypeOf({StripTop.class})
public @interface StripUpperOnly {}
