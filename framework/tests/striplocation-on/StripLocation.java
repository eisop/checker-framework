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

    // @StripBottom has no @TargetLocations, so the @TargetLocations-based mechanism never reports
    // or strips it here; StripLocationValidator's own tree-based rule (not derivable from
    // @TargetLocations) additionally forbids writing it explicitly at a lower-bound location, and
    // is only consulted when the opt-in is on.
    // :: error: (explicit.stripbottom.on.lowerbound)
    static class ExplicitBottomParam<@StripBottom T extends Object> {}

    // Relying on defaulting for the same lower-bound position is indistinguishable from the
    // @TargetLocations-based mechanism's perspective, but StripLocationValidator's tree-based check
    // (declTree has no explicit @StripBottom here) correctly does not report it.
    static class DefaultedBottomParam<T extends Object> {}

    // Same two cases for a wildcard's super (lower) bound.
    // :: error: (explicit.stripbottom.on.lowerbound)
    List<@StripBottom ? extends Object> explicitBottomWildcard() {
        return null;
    }

    List<? extends Object> defaultedBottomWildcard() {
        return null;
    }
}
