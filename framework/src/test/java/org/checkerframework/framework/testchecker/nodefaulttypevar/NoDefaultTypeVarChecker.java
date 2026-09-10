package org.checkerframework.framework.testchecker.nodefaulttypevar;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker that disables defaulting to reproduce and test the NullPointerException in
 * AnnotatedTypes.glbSubtype with unannotated type variable bounds.
 */
public class NoDefaultTypeVarChecker extends BaseTypeChecker {}
