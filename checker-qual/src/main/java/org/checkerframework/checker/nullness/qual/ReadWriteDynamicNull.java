package org.checkerframework.checker.nullness.qual;

import org.checkerframework.framework.qual.ReadWriteDynamicQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The unchecked-code default for a field, in the nullness type system. A programmer cannot write
 * this annotation; the Nullness Checker applies it to an unannotated field.
 *
 * <p>{@link ReadWriteDynamicNull} means {@link Nullable} where the field is read and {@link
 * NonNull} where the field is written, so that a value read from the field is not trusted and a
 * value stored into it is not weaker than the field's own type. It is applied only to an element
 * that conservative defaulting covers, so it always receives this sound interpretation.
 *
 * @checker_framework.manual #qualifier-read-write-sensitivity Read-write sensitivity
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@ReadWriteDynamicQualifier(NonNull.class)
public @interface ReadWriteDynamicNull {}
