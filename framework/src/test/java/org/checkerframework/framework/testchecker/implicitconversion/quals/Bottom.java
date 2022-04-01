package org.checkerframework.framework.testchecker.implicitconversion.quals;

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
        typeKinds = {
            TypeKind.INT,
            TypeKind.BYTE,
            TypeKind.SHORT,
            TypeKind.BOOLEAN,
            TypeKind.LONG,
            TypeKind.CHAR,
            TypeKind.FLOAT,
            TypeKind.DOUBLE
        },
        types = {
            String.class,
            Double.class,
            Boolean.class,
            Byte.class,
            Character.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class
        })
@DefaultFor(
        value = {TypeUseLocation.LOWER_BOUND},
        typeKinds = {
            TypeKind.INT,
            TypeKind.BYTE,
            TypeKind.SHORT,
            TypeKind.BOOLEAN,
            TypeKind.LONG,
            TypeKind.CHAR,
            TypeKind.FLOAT,
            TypeKind.DOUBLE
        },
        types = {
            String.class,
            Double.class,
            Boolean.class,
            Byte.class,
            Character.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class
        })
public @interface Bottom {}
