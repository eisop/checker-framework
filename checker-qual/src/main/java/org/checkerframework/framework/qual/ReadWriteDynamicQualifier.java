package org.checkerframework.framework.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A meta-annotation that indicates that an annotation is a read-write sensitive type qualifier.
 *
 * <p>A read-write dynamic qualifier means the top qualifier where the annotated expression is read
 * and the bottom qualifier where it is written, so that a value that is read is not trusted and a
 * value that is written is not weaker than the declared type. It is used as an unchecked-code
 * default, and such defaults are applied only to an element that conservative defaulting covers, so
 * it always receives this sound interpretation. A type system that wants an unsound but quieter
 * treatment of legacy code would need a separate command-line option; none exists today.
 *
 * <p>A read-write dynamic qualifier must not have a {@code @SubtypeOf} meta-annotation. It is
 * placed directly below the top of the hierarchy that contains {@link #value()}.
 *
 * @checker_framework.manual #qualifier-read-write-sensitivity Read-write sensitivity
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface ReadWriteDynamicQualifier {
    /**
     * A qualifier in the hierarchy that this read-write dynamic qualifier belongs to. It selects
     * the hierarchy; the dynamic qualifier is placed directly below that hierarchy's top.
     *
     * @return a qualifier in this read-write dynamic qualifier's hierarchy
     */
    Class<? extends Annotation> value();
}
