package org.checkerframework.framework.testchecker.elementdefault;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A checker used only to test {@link
 * org.checkerframework.framework.util.defaults.QualifierDefaults#addElementDefault}: its {@link
 * ElementDefaultAnnotatedTypeFactory} calls that method directly, rather than through a written
 * {@code @DefaultQualifier} annotation, on one package and two classes of {@code
 * framework/tests/elementdefault}'s test sources.
 *
 * <p>With {@code -AlateElementDefault}, the factory instead calls that method after type checking
 * has begun, which must be reported as a type-system error.
 */
@SupportedOptions(ElementDefaultAnnotatedTypeFactory.LATE_OPTION)
public final class ElementDefaultChecker extends BaseTypeChecker {}
