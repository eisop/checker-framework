package org.checkerframework.framework.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation used to indicate what lint options a checker supports. For example, if a checker
 * class (one that extends BaseTypeChecker) is annotated with
 * {@code @SupportedLintOptions({"dotequals"})}, then the checker accepts the command-line option
 * {@code -Alint=-dotequals}.
 *
 * <p>This annotation is optional and many checkers do not contain an {@code @SupportedLintOptions}
 * annotation.
 *
 * <p>{@link SourceChecker#getSupportedLintOptions} collects these annotations from the checker's
 * class hierarchy and from its subcheckers and parent checkers; write this annotation on the class
 * that handles the lint option.
 *
 * @see org.checkerframework.framework.source.SupportedOptions
 * @checker_framework.manual #creating-compiler-interface The checker class: Compiler interface
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SupportedLintOptions {
  String[] value();
}
