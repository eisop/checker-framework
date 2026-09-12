package org.checkerframework.framework.testchecker.elementdefault;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The top qualifier of the trivial two-qualifier hierarchy used only by {@link
 * ElementDefaultChecker}. {@link ElementDefaultAnnotatedTypeFactory#addCheckedCodeDefaults}
 * registers this as the checker-wide default explicitly, so this does not need
 * {@code @DefaultQualifierInHierarchy}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
public @interface ElementDefaultTop {}
