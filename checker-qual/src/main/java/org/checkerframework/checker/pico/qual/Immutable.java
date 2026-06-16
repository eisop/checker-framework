package org.checkerframework.checker.pico.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.UpperBoundFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Indicates that the referenced object is immutable.
 *
 * <p>When written on an object creation expression, the newly-created object is immutable. When
 * written on a reference type, the referenced object cannot be mutated. When written on a class
 * declaration, instances of that class have an immutable declaration bound.
 *
 * <p>PICO treats primitive values, string literals, and common wrapper/value classes as immutable.
 */
@SubtypeOf({Readonly.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(
        types = {
            Enum.class,
            String.class,
            Double.class,
            Boolean.class,
            Byte.class,
            Character.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            Number.class,
            BigDecimal.class,
            BigInteger.class
        },
        typeKinds = {
            TypeKind.INT,
            TypeKind.BYTE,
            TypeKind.SHORT,
            TypeKind.BOOLEAN,
            TypeKind.LONG,
            TypeKind.CHAR,
            TypeKind.FLOAT,
            TypeKind.DOUBLE
        })
@QualifierForLiterals({LiteralKind.PRIMITIVE, LiteralKind.STRING})
@UpperBoundFor(
        typeKinds = {
            TypeKind.INT, TypeKind.BYTE, TypeKind.SHORT, TypeKind.BOOLEAN,
            TypeKind.LONG, TypeKind.CHAR, TypeKind.FLOAT, TypeKind.DOUBLE
        },
        types = {
            Enum.class,
            String.class,
            Double.class,
            Boolean.class,
            Byte.class,
            Character.class,
            Float.class,
            Integer.class,
            Long.class,
            Short.class,
            Number.class,
            BigDecimal.class,
            BigInteger.class
        })
@HoldsForDefaultValue
public @interface Immutable {}
