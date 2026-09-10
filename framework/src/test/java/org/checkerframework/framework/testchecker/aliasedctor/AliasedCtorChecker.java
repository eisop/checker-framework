package org.checkerframework.framework.testchecker.aliasedctor;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker used only to test that an explicit, aliased annotation on a constructor's own declared
 * type is recognized when computing the type of a constructor reference ({@code Foo::new}), the
 * same way the canonical annotation would be. See {@code
 * org.checkerframework.framework.util.AnnotatedTypes#copyOnlyExplicitConstructorAnnotations}.
 */
public final class AliasedCtorChecker extends BaseTypeChecker {}
