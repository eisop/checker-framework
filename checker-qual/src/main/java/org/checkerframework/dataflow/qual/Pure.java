package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Pure} is a method annotation that means both {@link SideEffectFree} and {@link
 * Deterministic}. The more important of these, when performing pluggable type-checking, is usually
 * {@link SideEffectFree}.
 *
 * <p>For pluggable type-checking, {@link SideEffectFree} lets flow-sensitive type refinement keep
 * facts about the heap across a call to the method, and {@link Deterministic} lets it assume that a
 * later invocation of the method, with the same arguments and in the same environment, returns the
 * same value as an earlier one. Because a deterministic method that is not side-effect-free can
 * change the environment itself, both are needed for a fact learned about the result of one
 * invocation to hold for the result of the next; see {@link Deterministic} for an example.
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
