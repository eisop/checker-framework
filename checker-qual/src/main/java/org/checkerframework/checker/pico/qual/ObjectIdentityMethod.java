package org.checkerframework.checker.pico.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is an object identity method. An object identity method is a
 * method that returns the identity of the object it is called on.
 *
 * <p>For example, the {@code hashCode} method is an object identity method, because it returns the
 * identity of the object it is called on.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ObjectIdentityMethod {}
