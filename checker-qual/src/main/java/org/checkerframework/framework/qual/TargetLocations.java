package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that restricts the type-use locations where a type qualifier may be applied.
 * When written together with {@code @Target({ElementType.TYPE_USE})}, the given type qualifier may
 * be applied only at locations listed in the {@code @TargetLocations(...)} meta-annotation.
 * {@code @Target({ElementType.TYPE_USE})} together with no {@code @TargetLocations(...)} means that
 * the qualifier can be applied to any type use. {@code @TargetLocations({})} means that the
 * qualifier should not be used in source code. The same goal can be achieved by writing
 * {@code @Target({})}, which is enforced by javac itself. {@code @TargetLocations({...})} is
 * enforced by the checker. The resulting errors from the checker can either be suppressed using
 * {@code @SuppressWarnings("type.invalid.annotations.on.location")} or can be ignored by providing
 * the {@code -AignoreTargetLocations} option.
 *
 * <p>This enables a type system designer to permit a qualifier to be applied only in certain
 * locations. For example, some type systems' top and bottom qualifier (such as {@link
 * org.checkerframework.checker.regex.qual.RegexBottom}) should only be written on an explicit
 * wildcard upper or lower bound. This meta-annotation is a declarative, coarse-grained approach to
 * enable that. For finer-grained control, override {@code visit*} methods that visit trees in
 * BaseTypeVisitor.
 *
 * <p>{@code @TargetLocations} are used for all appearances of qualifiers regardless of whether they
 * are provided explicitly or implicitly (inferred or computed). Therefore, only use type-use
 * locations {@code LOWER_BOUND/UPPER_BOUND} instead of the {@code IMPLICIT_XX/EXPLICIT_XX}
 * alternatives.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TargetLocations {
    /**
     * Type uses at which the qualifier is permitted to be applied in source code.
     *
     * @return type-use locations declared in this meta-annotation
     */
    TypeUseLocation[] value();
}
