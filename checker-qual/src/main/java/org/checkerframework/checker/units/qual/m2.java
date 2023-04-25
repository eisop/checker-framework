package org.checkerframework.checker.units.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Square meter.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Area.class)
@SuppressWarnings("checkstyle:typename")
public @interface m2 {
  // does this make sense? Is it multiple of (m^2)? Or (multiple of m)^2?
  Prefix value() default Prefix.one;
}
