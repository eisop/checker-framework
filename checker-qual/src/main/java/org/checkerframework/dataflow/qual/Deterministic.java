package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method is called <em>deterministic</em> if, starting from the same arguments and the same
 * initial environment, every invocation produces the same return value (according to {@code ==})
 * and the same final environment. The arguments include the receiver, and the environment
 * includes all of the Java heap (that is, all fields of all objects and all static variables).
 *
 * <p>Determinism does <em>not</em> mean that the method leaves the heap unchanged; it means that
 * any changes the method makes to the heap are the same on every invocation from the same
 * starting heap. For example, a method whose body always executes {@code this.f = 99; return 5;}
 * is deterministic, even though it is not {@link SideEffectFree}. (The Checker Framework's
 * conservative analysis described below is currently stricter than this — see the note in the
 * Analysis section.)
 *
 * <p>Determinism refers to the return value (and the final environment) during a non-exceptional
 * execution. If a method throws an exception, the Throwable does not have to be exactly the same
 * object on each invocation (and generally should not be, to capture the correct stack trace).
 *
 * <p><b>Use in flow-sensitive type refinement:</b> By itself, {@code @Deterministic} provides only
 * a limited guarantee. Two consecutive calls to the same {@code @Deterministic} method are not
 * guaranteed to return the same value, because the first call may have changed the heap, so the
 * second call no longer starts from the same environment. For example, the following method is
 * deterministic:
 *
 * <pre>{@code
 * @Deterministic
 * Object myDeterministicMethod() {
 *   Object o = this.f;
 *   this.f = null;
 *   return o;
 * }
 * }</pre>
 *
 * but a type refinement on the result of a first call does not survive a second call:
 *
 * <pre>{@code
 * if (x.myDeterministicMethod() != null) {
 *   x.myDeterministicMethod().hashCode(); // throws NullPointerException
 * }
 * }</pre>
 *
 * because the first call sets {@code this.f} to {@code null}, so the second call returns
 * {@code null}. To get the guarantee that any property inferred from one call also holds at a
 * subsequent call to the same method, the method must be both {@code @Deterministic} and
 * {@link SideEffectFree} — that is, {@link Pure}.
 *
 * <p>Note that {@code @Deterministic} guarantees that the result is identical according to {@code
 * ==}, <b>not</b> just equal according to {@code equals()}. This means that writing
 * {@code @Deterministic} on a method that returns a reference (including a String) is often
 * erroneous unless the returned value is cached or interned.
 *
 * <p>Also see {@link Pure}, which means both deterministic and {@link SideEffectFree}.
 *
 * <p><b>Analysis:</b> The Checker Framework performs a conservative analysis to verify a
 * {@code @Deterministic} annotation. The Checker Framework issues a warning if the method uses any
 * of the following Java constructs:
 *
 * <ol>
 *   <li>Assignment to any expression, except for local variables and method parameters.<br>
 *       (Note that storing into an array element, such a {@code a[i] = x}, is not an assignment to
 *       a variable and is therefore forbidden.)
 *   <li>A method invocation of a method that is not {@link Deterministic}.
 *   <li>Construction of a new object.
 *   <li>Catching any exceptions. This restriction prevents a method from obtaining a reference to a
 *       newly-created exception object and using these objects (or some property thereof) to change
 *       the method's return value. For instance, the following method must be forbidden.
 *       <!-- "<code>" instead of "{@code ...}" because of at-sign at beginning of line -->
 *       <pre><code>@Deterministic
 * int f() {
 *   try {
 *     int b = 0;
 *     int a = 1/b;
 *   } catch (Throwable t) {
 *     return t.hashCode();
 *   }
 *   return 0;
 * }
 * </code></pre>
 * </ol>
 *
 * When a constructor is annotated as {@code Deterministic} (or {@code @Pure}), that means that all
 * the fields are deterministic (the same values, if the arguments are the same). The constructed
 * object itself is different. That is, a constructor <em>invocation</em> is never deterministic
 * since it returns a different new object each time.
 *
 * <p>Note that the rules for checking currently imply that every {@code Deterministic} method is
 * also {@link SideEffectFree}. This might change in the future; in general, a deterministic method
 * does not need to be side-effect-free.
 *
 * <p>These rules are conservative: any code that passes the checks is deterministic, but the
 * Checker Framework may issue false positive warnings, for code that uses one of the forbidden
 * constructs but is deterministic nonetheless.
 *
 * <p>In fact, the rules are so conservative that checking is currently disabled by default, but can
 * be enabled via the {@code -AcheckPurityAnnotations} command-line option.
 *
 * <p>This annotation is inherited by subtypes, just as if it were meta-annotated with
 * {@code @InheritedAnnotation}.
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
// @InheritedAnnotation cannot be written here, because "dataflow" project cannot depend on
// "framework" project.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Deterministic {}
