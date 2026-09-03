package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this class has not been annotated for the given type system, even though an
 * enclosing element is annotated for it. For example, if a package is
 * {@code @AnnotatedFor("nullness")} but one of its classes has not been annotated with
 * {@code @Nullable} and friends, mark that class {@code @UnannotatedFor("nullness")}. The argument
 * to {@code UnannotatedFor} is not an annotation name, but a checker name.
 *
 * <p>This annotation has no effect unless the {@code
 * -AuseConservativeDefaultsForUncheckedCode=source} command-line argument is supplied. It only
 * subtracts from the scope of an enclosing {@link AnnotatedFor}: an element in its scope is
 * defaulted using conservative defaults and its warnings are suppressed, as if no enclosing
 * {@code @AnnotatedFor} were present. An {@code @AnnotatedFor} on a nested element takes effect
 * again for that element.
 *
 * @checker_framework.manual #compiling-libraries Compiling partially-annotated libraries
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface UnannotatedFor {
    /**
     * Returns the type systems for which the class has not been annotated. Legal arguments are any
     * string that may be passed to the {@code -processor} command-line argument: the
     * fully-qualified class name for the checker, or a shorthand for built-in checkers. Using the
     * annotation with no arguments, as in {@code @UnannotatedFor({})}, has no effect.
     *
     * @return the type systems for which the class has not been annotated
     * @checker_framework.manual #shorthand-for-checkers Short names for built-in checkers
     */
    String[] value();
}
