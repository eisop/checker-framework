package org.checkerframework.framework.testchecker.elementdefault;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker used only to test {@link
 * org.checkerframework.framework.util.defaults.QualifierDefaults#addElementDefault}: its {@link
 * ElementDefaultAnnotatedTypeFactory} calls that method directly, rather than through a written
 * {@code @DefaultQualifier} annotation, on one specific package (named in {@code
 * framework/tests/elementdefault}'s test source), to confirm the default reaches that package's
 * subpackages the same way a written annotation would.
 */
public final class ElementDefaultChecker extends BaseTypeChecker {}
