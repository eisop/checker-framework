package org.checkerframework.framework.testchecker.h1h2checker.quals;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({
    TypeUseLocation.FIELD,
    TypeUseLocation.LOCAL_VARIABLE,
    TypeUseLocation.PARAMETER,
    TypeUseLocation.RETURN,
    TypeUseLocation.CONSTRUCTOR_RESULT
})
@SubtypeOf({H2Top.class})
public @interface H2S1 {}
