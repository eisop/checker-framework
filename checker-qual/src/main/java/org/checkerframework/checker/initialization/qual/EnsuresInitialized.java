package org.checkerframework.checker.initialization.qual;

import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.PostconditionAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method postcondition annotation that guarantees that the given Java expressions are {@link
 * Initialized} after the method terminates successfully.
 *
 * <p>Note: This is an Initialization Checker postcondition, distinct from {@link
 * org.checkerframework.common.initializedfields.qual.EnsuresInitializedFields} which is used by the
 * standalone Initialized Fields Checker.
 *
 * @checker_framework.manual #initialization-checker Initialization Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@PostconditionAnnotation(qualifier = Initialized.class)
@InheritedAnnotation
@Repeatable(EnsuresInitialized.List.class)
public @interface EnsuresInitialized {
    /**
     * The Java expressions that are initialized upon successful method termination.
     *
     * @return the Java expressions that are initialized
     * @checker_framework.manual #java-expressions-as-arguments Syntax of Java expressions
     */
    String[] value();

    /** A wrapper annotation that makes the {@link EnsuresInitialized} annotation repeatable. */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
    @PostconditionAnnotation(qualifier = Initialized.class)
    @InheritedAnnotation
    public static @interface List {
        /**
         * Return the repeatable annotations.
         *
         * @return the repeatable annotations
         */
        EnsuresInitialized[] value();
    }
}
