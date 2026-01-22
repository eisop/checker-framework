package org.checkerframework.checker.pico.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @PICOBottom} can only be annotated before a type parameter (For example, {@code class
 * C<@Bottom T extends MutableBox>{}}). This means {@code @PICOBottom} is the lower bound for this
 * type parameter.
 *
 * <p>User can explicitly write {@code @PICOBottom} but it's not necessary because it's
 * automatically inferred, and we have -AwarnRedundantAnnotations flag to warn about redundant
 * annotations.
 */
@SubtypeOf({Mutable.class, Immutable.class, ReceiverDependentMutable.class, PICOLost.class})
@DefaultFor(typeKinds = {TypeKind.NULL})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.LOWER_BOUND})
public @interface PICOBottom {}
