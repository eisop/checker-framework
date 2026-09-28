package org.checkerframework.framework.testchecker.aliasedctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An alias for {@link AliasedCtorBottom}, registered programmatically by {@link
 * AliasedCtorAnnotatedTypeFactory} via {@code addAliasedTypeAnnotation} rather than by a
 * meta-annotation. Deliberately not part of the qualifier hierarchy itself (no {@code @SubtypeOf}):
 * {@code addAliasedTypeAnnotation} rejects an alias that is also a supported qualifier.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface AliasedCtorLegacyBottom {}
