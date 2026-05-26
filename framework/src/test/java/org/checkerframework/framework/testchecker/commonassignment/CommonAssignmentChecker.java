package org.checkerframework.framework.testchecker.commonassignment;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A test checker used to exercise the contract of {@link
 * org.checkerframework.common.basetype.BaseTypeVisitor#commonAssignmentCheck(com.sun.source.tree.Tree,
 * com.sun.source.tree.ExpressionTree, String, Object[])}.
 *
 * <p>This checker is paired with a custom {@link
 * org.checkerframework.common.basetype.BaseTypeValidator} that flags types annotated with
 * {@code @CommonInvalid} as invalid. When the validator marks a type as invalid, {@code
 * commonAssignmentCheck(Tree, ...)} must return {@code false}; otherwise, subclass overrides that
 * compose results with {@code &&} would silently treat such assignments as valid.
 */
public class CommonAssignmentChecker extends BaseTypeChecker {}
