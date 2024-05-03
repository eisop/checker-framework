package org.checkerframework.checker.immutability.qual;

import org.checkerframework.checker.initialization.qual.HoldsForDefaultValue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@HoldsForDefaultValue
public @interface Assignable {}
