package org.checkerframework.framework.testchecker.lhsexception;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker whose type factory throws while computing the type of the left-hand side of an
 * assignment to a variable named {@code throwInLhs}, to test that the factory still uses
 * flow-sensitive refinement in the compilation units that follow.
 *
 * <p>It uses the type system of {@link
 * org.checkerframework.framework.testchecker.util.FlowTestChecker} only to inherit a working
 * qualifier hierarchy with flow-sensitive refinement.
 */
public class LhsExceptionChecker extends BaseTypeChecker {}
