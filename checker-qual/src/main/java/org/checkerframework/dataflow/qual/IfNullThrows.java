package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A parameter annotation indicating that the method throws an exception if this parameter is null.
 *
 * <p>When the CFG is built, calls to methods with {@code @IfNullThrows} on a parameter are
 * translated into an explicit branch: if the argument is null, the method throws; otherwise
 * execution continues. This enables flow-sensitive refinement in type checkers (e.g., the Nullness
 * Checker refines the argument to non-null on the continue path).
 *
 * <p><b>Semantic meaning:</b> {@code @IfNullThrows} means: <i>if this parameter is null, then the
 * method throws</i>. Equivalently: when the method returns normally, the parameter was non-null.
 *
 * <p><b>Example:</b>
 *
 * <pre><code>
 * public static &lt;T&gt; T requireNonNull(@IfNullThrows @Nullable T obj) {
 *   if (obj == null) throw new NullPointerException();
 *   return obj;
 * }
 * </code></pre>
 *
 * @checker_framework.manual #type-refinement Automatic type refinement (flow-sensitive type
 *     qualifier inference)
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface IfNullThrows {}
