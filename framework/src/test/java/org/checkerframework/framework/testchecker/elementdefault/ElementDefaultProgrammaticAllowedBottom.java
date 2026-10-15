package org.checkerframework.framework.testchecker.elementdefault;

import org.checkerframework.framework.qual.ProgrammaticDefaultLocations;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A qualifier with restricted @TargetLocations, but with @ProgrammaticDefaultLocations allowing all
 * locations for programmatic defaults.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.PARAMETER, TypeUseLocation.EXPLICIT_LOWER_BOUND})
@ProgrammaticDefaultLocations
@SubtypeOf(ElementDefaultRestrictedBottom.class)
public @interface ElementDefaultProgrammaticAllowedBottom {}
