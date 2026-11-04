package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is for comments related to the Checker Framework.
 *
 * <p>Using {@code @CFComment} makes it easy to find Checker-Framework-related comments, and to copy
 * those comments from one version of a codebase to another (using the Annotation File Utilities).
 *
 * <p>Here is an example:
 *
 * <pre><code>
 * {@literal @}CFComment("interning: factory methods guarantee that all elements are interned")
 *  public class MyClass {
 *   {@literal @}CFComment({"nullness: non-null return type is more specific than in superclass",
 *                "signedness: comment related to Signedness type system"})
 *    public String myMethod() { ... }
 * }
 * </code></pre>
 *
 * <p>As a matter of style, programmers should use this annotation on the most deeply nested element
 * to which the comment applies (e.g., local variable rather than method, and method rather than
 * class).
 *
 * <p>This annotation has source retention: its comments are for people who read and maintain the
 * source code, and the Checker Framework does not interpret them, so they are not stored in class
 * files.
 *
 * @checker_framework.manual #library-tips-dont-change-the-code Don't change the code
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
public @interface CFComment {
    /**
     * Comments about Checker Framework annotations. The text is not interpreted by the Checker
     * Framework.
     *
     * <p>If you prefix each comment by the name of the type system, the comments are easier to
     * understand and search for.
     */
    String[] value();
}
