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
 * <p>Any method written using {@link PolyInitialized} takes a class argument and is instantiated
 * for its possible value.
 *
 * <p>The possible value could be:
 *
 * @see UnknownInitialization
 * @see UnderInitialization
 * @see Initialized
 * @checker_framework.manual #initialization-checker Initialization Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownInitialization.class)
public @interface PolyInitialized {}
