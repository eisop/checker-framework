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
 * <p>Any method written using {@link PolyRegex} has multiple versions, every instance of which can
 * be replaced by one of following qualifier: {@link Regex}, which can take an integer argument to
 * represent different capturing groups; {@link PartialRegex}, which can take string argument to
 * represent different partial regexes; {@link UnknownRegex} and {@link RegexBottom}.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownRegex.class)
public @interface PolyRegex {}
