package org.jmlspecs.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A copy of JML's {@code Pure} annotation, so the Checker Framework can refer to it. It is treated
 * as an alias of {@link org.checkerframework.dataflow.qual.Pure}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pure {}
