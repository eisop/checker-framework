package org.checkerframework.framework.testchecker.commonassignment.quals;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker qualifier for test types that should be flagged as invalid by the type validator.
 *
 * <p>The custom type validator reports these types as invalid via {@code reportInvalidType}, which
 * makes {@code BaseTypeVisitor#validateType} return {@code false}. This is what exercises the path
 * of {@code commonAssignmentCheck(Tree, ExpressionTree, ...)} that previously returned {@code true}
 * after a failed validation and now correctly returns {@code false}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({CommonTop.class})
public @interface CommonInvalid {}
