package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation can be used on a type variable use to
 * explicitly indicate that the type variable is parametric in that type hierarchy.
 *
 * <p>This is useful in combination with defaults for {@link TypeUseLocation#TYPE_VARIABLE_USE}, as
 * it provides a way to have a default qualifier that expresses parametricity, which is usually
 * expressed by the absence of an annotation on the type variable use.
 *
 * <p>An annotation meta-annotated with {@code ParametricTypeVariableUseQualifier} will usually not
 * be written explicitly in source code.
 *
 * <p>This annotation is currently only for documentation, but will in the future also be used for
 * automatic support for parametric type variable use qualifiers.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@AnnotatedFor("nullness")
public @interface ParametricTypeVariableUseQualifier {
    /**
     * Indicates which type system this annotation refers to (optional, and usually unnecessary).
     * When multiple type hierarchies are supported by a single type system, then each parametric
     * qualifier needs to indicate which sub-hierarchy it belongs to. Do so by passing the top
     * qualifier from the given hierarchy.
     *
     * @return the top qualifier in the hierarchy of this qualifier
     */
    // We use the meaningless Annotation.class as default value and
    // then ensure there is a single top qualifier to use.
    Class<? extends Annotation> value() default Annotation.class;
}
