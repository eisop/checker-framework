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
 * the qualifier can be applied on any type use. {@code @TargetLocations({})} will prevent the
 * annotation from been applied in the source code, but you can achieve the same goal by writing
 * {@code @Target({})}. So, we don't suggest writing the annotation with no type-use location
 * supplied.
 *
 * <p>This enables a type system designer to permit a qualifier to be applied only in certain
 * locations. For example, some type systems' top and bottom qualifier (such as {@link
 * org.checkerframework.checker.regex.qual.RegexBottom}) should only be written on an explicit
 * wildcard upper or lower bound. This meta-annotation is a declarative, coarse-grained approach to
 * enable that. For finer-grained control, override {@code visit*} methods that visit trees in
 * BaseTypeVisitor.
 *
 * <p>This meta-annotation prevents the type system from inferring, or computing the given qualifier
 * at the given location. It also prevents users from writing that qualifier explicitly at the given
 * location.
 *
 * <p>Because the meta-annotation avoids given annotations implicitly and explicitly applying on
 * certain locations, supply LOWER_BOUND (UPPER_BOUND) rather than EXPLICIT_LOWER_BOUND
 * (EXPLICIT_UPPER_BOUND) as the value of TargetLocations.
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
