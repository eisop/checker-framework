package org.checkerframework.checker.initialization.qual;

import org.checkerframework.checker.regex.qual.UnknownRegex;
import org.checkerframework.framework.qual.PolymorphicQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A polymorphic qualifier for the freedom-before-commitment initialization tracking type-system.
 *
 * <p>Any type annotated by {@link PolyInitialized} conceptually can be replaced by the following
 * qualifiers: {@link Initialized}; {@link UnknownRegex}; {@link UnderInitialization}, which can
 * take a class argument and instantiate to different type frames.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownInitialization.class)
public @interface PolyInitialized {}
