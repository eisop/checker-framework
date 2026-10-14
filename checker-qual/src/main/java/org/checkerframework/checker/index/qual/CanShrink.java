package org.checkerframework.checker.index.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An expression of type {@code @CanShrink} may be used to remove elements, e.g., by calling {@code
 * remove()} or {@code clear()} on it.
 */
// Reinstate when lists are supported:
//  * @checker_framework.manual #growonly-checker Grow-only Checker
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnshrinkableRef.class})
public @interface CanShrink {}
