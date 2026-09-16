package org.checkerframework.framework.testchecker.aliasedctor;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation that deliberately declares no {@code @Target} meta-annotation, and is therefore not
 * a type qualifier: without {@code @Target} it is applicable in every declaration context.
 *
 * <p>Used only to exercise the two places that reject such an annotation -- {@code
 * AnnotatedTypeFactory.checkSupportedQualsAreTypeQuals}, when a type system declares one as a
 * supported qualifier, and the {@code -AaliasedTypeAnnos} option, when a user names one as a
 * canonical annotation. Both would otherwise dereference a null {@code @Target}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@SubtypeOf(AliasedCtorTop.class)
public @interface AliasedCtorNoTarget {}
