package org.checkerframework.framework.testchecker.striplocation;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * A test checker whose {@link
 * org.checkerframework.framework.testchecker.striplocation.quals.StripUpperOnly} qualifier declares
 * restrictive {@link org.checkerframework.framework.qual.TargetLocations}. When passed {@code
 * -AstripInvalidLocationQualifiers}, it opts in to making location-invalid qualifiers on bounds
 * inert; otherwise it exhibits the default behavior (the qualifier is reported but still takes
 * effect, producing a {@code bound.type.incompatible} cascade).
 */
@SupportedOptions({"stripInvalidLocationQualifiers"})
public class StripLocationChecker extends BaseTypeChecker {}
