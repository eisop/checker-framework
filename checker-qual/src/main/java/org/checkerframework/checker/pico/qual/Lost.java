package org.checkerframework.checker.pico.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.*;

/** Lost means the mutability type of this reference is unknown. This is a subtype of Readonly. */
@SubtypeOf({Readonly.class})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Lost {}
