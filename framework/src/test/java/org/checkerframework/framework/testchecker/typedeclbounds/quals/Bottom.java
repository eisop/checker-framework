package org.checkerframework.framework.testchecker.typedeclbounds.quals;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.UpperBoundFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Toy type system for testing impact of implicit java type conversion.
 *
 * @see Top
 */
@SubtypeOf({Top.class})
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@QualifierForLiterals({LiteralKind.ALL})
@UpperBoundFor(
        typeKinds = {TypeKind.BOOLEAN},
        types = {String.class})
@DefaultFor(
        value = {TypeUseLocation.LOWER_BOUND},
        typeKinds = {TypeKind.BOOLEAN},
        types = {String.class})
public @interface Bottom {}
