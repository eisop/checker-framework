package org.checkerframework.framework.testchecker.aliasedctor;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A checker used only to test that an explicit, aliased annotation on a constructor's own declared
 * type is recognized when computing the type of a constructor reference ({@code Foo::new}), the
 * same way the canonical annotation would be. See {@code
 * org.checkerframework.framework.util.AnnotatedTypes#copyOnlyExplicitConstructorAnnotations}.
 *
 * <p>Also tests validation in {@code addAliasedTypeAnnotation}.
 */
@SupportedOptions({
    AliasedCtorAnnotatedTypeFactory.UNSUPPORTED_CANONICAL_MIRROR_OPTION,
    AliasedCtorAnnotatedTypeFactory.UNSUPPORTED_CANONICAL_CLASS_OPTION,
    AliasedCtorAnnotatedTypeFactory.ALIAS_IS_QUALIFIER_NAME_OPTION,
    AliasedCtorAnnotatedTypeFactory.ALIAS_IS_QUALIFIER_CLASS_OPTION,
    AliasedCtorAnnotatedTypeFactory.NO_TARGET_QUALIFIER_OPTION
})
public final class AliasedCtorChecker extends BaseTypeChecker {}
