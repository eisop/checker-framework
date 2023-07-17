package org.checkerframework.checker.initialization.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A polymorphic qualifier for the freedom-before-commitment initialization tracking type-system.
 *
 * <p>All types annotated with {@link PolyInitialized} conceptually can be replaced by only one of
 * the following qualifier: {@link Initialized}, {@link UnknownInitialization} and {@link
 * UnderInitialization} which take a class argument to represent different type frames and {@link
 * FBCBottom}.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownInitialization.class)
public @interface PolyInitialized {}
