package org.checkerframework.framework.testchecker.nodefaulttypevar.quals;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** The bottom type qualifier for the NoDefaultTypeVarChecker type system. */
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Top.class})
public @interface Bottom {}
