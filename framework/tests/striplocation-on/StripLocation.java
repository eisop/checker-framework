import org.checkerframework.framework.testchecker.striplocation.quals.StripBottom;
import org.checkerframework.framework.testchecker.striplocation.quals.StripUpperOnly;

import java.util.List;

// Opt-in on (-AstripInvalidLocationQualifiers): a qualifier written at a location its
// @TargetLocations forbids is still reported, but it is stripped from the bound and the bound is
// re-defaulted, so no bound.type.incompatible cascade is reported.
public class StripLocation {

    // Valid use: @StripUpperOnly is permitted on an upper bound, so there is no error.
    static class ValidUpper<T extends @StripUpperOnly Object> {}

    // @StripUpperOnly on a type-parameter (lower-bound) location is invalid.  With the opt-in on
    // the
    // qualifier is stripped, so only the location error remains (no bound.type.incompatible).
    // :: error: (type.invalid.annotations.on.location)
    static class BadParam<@StripUpperOnly T extends @StripBottom Object> {}

    // @StripUpperOnly on a wildcard super (lower) bound location is invalid.  With the opt-in on
    // the
    // qualifier is stripped, so only the location error remains (no bound.type.incompatible).
    // :: error: (type.invalid.annotations.on.location)
    List<@StripUpperOnly ? extends @StripBottom Object> badWildcard() {
        return null;
    }
}
