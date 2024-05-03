package org.checkerframework.checker.immutability.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;
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
@PolymorphicQualifier(Readonly.class)
@TargetLocations({
    TypeUseLocation.PARAMETER,
    TypeUseLocation.RECEIVER,
    TypeUseLocation.RETURN,
    TypeUseLocation.LOCAL_VARIABLE
})
public @interface PolyMutable {}
