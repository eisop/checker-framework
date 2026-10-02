package org.checkerframework.framework.testchecker.nodefaulttypevar.quals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/** The bottom type qualifier for the NoDefaultTypeVarChecker type system. */
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Top.class})
public @interface Bottom {}
