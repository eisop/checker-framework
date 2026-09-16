package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that means both {@link SideEffectFree} and {@link
 * Deterministic}.
 *
 * <p>For pluggable type-checking, the two components play complementary roles:
 *
 * <ul>
 *   <li>{@link SideEffectFree} preserves facts <em>about heap</em> across the call: a
 *       property known about a field before the call is still known afterwards.
 *   <li>{@link Deterministic} ensures that, from the same starting state, the call always returns
 *       the same value. By itself this is not enough to assume a property checked on the result of
 *       one call also holds on the result of a subsequent call to the same method, because a
 *       deterministic method may make state changes that invalidate its own precondition on the
 *       second call (see {@link Deterministic} for an example).
 * </ul>
 *
 * Only {@code @Pure} provides both guarantees together: facts inferred from a {@code @Pure} call
 * survive across the call and across subsequent calls to the same method with the same arguments
 * and receiver, provided those subsequent calls begin in the same relevant starting state (that
 * is, with no intervening state changes that affect the method's result).
 *
 * <p>For a discussion of the meaning of {@code Pure} on a constructor, see the documentation of
 * {@link Deterministic}.
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
public @interface Pure {
    /** The type of purity. */
    enum Kind {
        /** The method has no visible side effects. */
        SIDE_EFFECT_FREE,

        /** The method returns exactly the same value when called in the same environment. */
        DETERMINISTIC
    }
}
