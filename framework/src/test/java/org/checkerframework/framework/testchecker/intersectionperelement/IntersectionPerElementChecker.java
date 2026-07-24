package org.checkerframework.framework.testchecker.intersectionperelement;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A test checker that opts into per-element intersection-bound semantics by having its type factory
 * override {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#shouldHomogenizeIntersectionBounds()} to
 * return false. It reuses the H1/H2 qualifier hierarchies of the {@code h1h2checker} test checker.
 */
public class IntersectionPerElementChecker extends BaseTypeChecker {}
