package org.checkerframework.framework.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that specifies the type-use locations where a type qualifier is permitted to be
 * used as a programmatic default (e.g. by {@link
 * org.checkerframework.framework.util.defaults.QualifierDefaults#addCheckedCodeDefault}, {@link
 * org.checkerframework.framework.util.defaults.QualifierDefaults#addUncheckedCodeDefault}, or
 * {@link org.checkerframework.framework.util.defaults.QualifierDefaults#addElementDefault}), even
 * if the qualifier's {@link TargetLocations} meta-annotation prohibits explicit use by programmers
 * at those locations.
 *
 * <p>If used as a marker annotation without arguments (the default), the qualifier is permitted as
 * a programmatic default at all type-use locations ({@link TypeUseLocation#ALL}).
 *
 * <p>If a qualifier has no {@code @ProgrammaticDefaultLocations} annotation, top and bottom
 * qualifiers in a qualifier hierarchy are permitted as programmatic defaults at all locations by
 * default. Other qualifiers are permitted only at the locations allowed by their {@link
 * TargetLocations} meta-annotation (or all locations if {@link TargetLocations} is also omitted).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ProgrammaticDefaultLocations {
    /**
     * Type-use locations at which the qualifier is permitted as a programmatic default.
     *
     * @return type-use locations
     */
    TypeUseLocation[] value() default {TypeUseLocation.ALL};
}
