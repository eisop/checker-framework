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
 * The polymorphic qualifier {@code @PolyMutable} indicates that the mutability type of this
 * reference can be substituted to {@code @Mutable} or {@code @Immutable} or {@code @RDM}. This is a
 * polymorphic qualifier that can be used in the type hierarchy.
 *
 * <p>{@code @PolyMutable} applies to method parameters, method return type and receiver.
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
