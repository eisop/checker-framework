package org.checkerframework.checker.mutability.qual;

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
 * The bottom qualifier in the mutability hierarchy.
 *
 * <p>{@code @MutabilityBottom} is used as the lower bound for type parameters and as the qualifier
 * for the null literal. It is inferred by the checker and normally does not need to be written in
 * source code.
 */
@SubtypeOf({Mutable.class, Immutable.class, ReceiverDependentMutable.class, MutabilityLost.class})
@DefaultFor(typeKinds = {TypeKind.NULL})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.LOWER_BOUND})
public @interface MutabilityBottom {}
