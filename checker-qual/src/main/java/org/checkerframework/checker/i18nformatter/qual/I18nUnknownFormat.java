package org.checkerframework.checker.i18nformatter.qual;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top qualifier.
 *
 * <p>A type annotation indicating that the run-time value might or might not be a valid i18n format
 * string.
 *
 * <p>It is usually not necessary to write this annotation in source code. It is an implementation
 * detail of the checker.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.ALL})
@InvisibleQualifier
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface I18nUnknownFormat {}
