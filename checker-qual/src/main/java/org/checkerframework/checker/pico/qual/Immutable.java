package org.checkerframework.checker.pico.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeKind;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.UpperBoundFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * {@code @Immutable} is a type qualifier that indicates that the fields of annotated reference
 * cannot be mutated.
 *
 * <p>For usage in PICO, there are three ways to use this annotation: Object creation: the object
 * created will always be immutable; Annotation on a reference: the object that reference points to
 * is immutable; Annotation on a class: all instances of that class are immutable.
 */
@SubtypeOf({Readonly.class})
@Documented
@DefaultQualifierInHierarchy
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(
        value = {TypeUseLocation.EXCEPTION_PARAMETER},
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
