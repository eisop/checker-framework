package org.checkerframework.framework.testchecker.aliasedctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The bottom qualifier of the trivial two-qualifier hierarchy used only by {@link
 * AliasedCtorChecker}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(AliasedCtorTop.class)
public @interface AliasedCtorBottom {}
