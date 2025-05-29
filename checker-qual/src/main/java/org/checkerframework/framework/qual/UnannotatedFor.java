package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this class has not been annotated for the given type system and this annotation is
 * used to exclude package, class or method which already in {@code @Annotatedfor} scope. In the
 * scope of {@code UnannotatedFor}, the source code and bytecode should use conservative default if
 * the command-line argument {@code -AuseConservativeDefaultsForUncheckedCode=source} is supplied
 * while other package, class or method in {@code @Annotatedfor} scope is defaulted normally
 * (typically using the CLIMB-to-top rule).
 *
 * <p>For example, mark a package as @Annotatedfor("nullness") will indicate that the package has
 * been annotated with nullness annotation but some classes or methods in the package is not
 * annotated, then mark the class or method with @UnannotatedFor("nullness") to exclude them from
 * the scope of @Annotatedfor("nullness").
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface UnannotatedFor {
    String[] value();
}
