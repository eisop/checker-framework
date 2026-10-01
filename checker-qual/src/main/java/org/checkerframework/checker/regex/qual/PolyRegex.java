package org.checkerframework.checker.regex.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A polymorphic qualifier for the Regex type system.
 *
 * <p>Any method written using {@link PolyRegex} conceptually has multiple versions: one in which
 * all instances of {@link PolyRegex} in the method signature have been replaced by one of the
 * following qualifiers: {@link Regex}, which takes an integer argument to represent different
 * capturing groups; {@link PartialRegex}, which takes a string argument to represent different
 * partial regexes; {@link UnknownRegex}; and {@link RegexBottom}.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownRegex.class)
public @interface PolyRegex {}
