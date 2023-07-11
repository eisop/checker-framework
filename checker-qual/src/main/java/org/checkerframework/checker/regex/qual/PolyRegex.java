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
 * <p>Any type annotated by {@link PolyRegex} in the method signature conceptually can be replaced
 * by the same following qualifiers: {@link Regex}, which can take an integer argument and
 * instantiate to different capturing group argument; {@link UnknownRegex}; {@link PartialRegex},
 * which can take string argument and instantiate to different partial regex; {@link UnknownRegex};
 * {@link RegexBottom}.
 *
 * @checker_framework.manual #regex-checker Regex Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownRegex.class)
public @interface PolyRegex {}
