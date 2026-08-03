import org.checkerframework.framework.testchecker.striplocation.quals.StripBottom;
import org.checkerframework.framework.testchecker.striplocation.quals.StripTop;
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

    // @StripTop has no @TargetLocations, so the @TargetLocations-based mechanism never reports or
    // strips it here; StripLocationValidator's own tree-based rule (not derivable from
    // @TargetLocations) additionally forbids writing it explicitly at a lower-bound location, and
    // is
    // only consulted when the opt-in is on. Stripping it here has an observable effect: without it,
    // an explicit @StripTop lower bound is incompatible with the @StripBottom upper bound (see the
    // striplocation-off fixture); after stripping and re-defaulting to @StripBottom here, no
    // cascade
    // is reported.
    // :: error: (explicit.striptop.on.lowerbound)
    static class ExplicitTopParam<@StripTop T extends @StripBottom Object> {}

    // Relying on defaulting for the same lower-bound position is indistinguishable from the
    // @TargetLocations-based mechanism's perspective, but StripLocationValidator's tree-based check
    // (declTree has no explicit @StripTop here) correctly does not report it.
    static class DefaultedTopParam<T extends @StripBottom Object> {}

    // Same two cases for a wildcard's super (lower) bound.
    // :: error: (explicit.striptop.on.lowerbound)
    List<@StripTop ? extends @StripBottom Object> explicitTopWildcard() {
        return null;
    }

    List<? extends @StripBottom Object> defaultedTopWildcard() {
        return null;
    }

    // The ? super form: the explicit lower bound is written after "super" rather than as a primary
    // annotation on "?".  StripLocationValidator finds it via the bound tree instead, exercising
    // the
    // ? super tree shape.  No cascade is possible either way here (an explicit @StripTop super
    // bound
    // is always compatible with the implicit, defaulted-to-top extends bound), so only the report
    // differs from the (untested) unguarded case.
    // :: error: (explicit.striptop.on.lowerbound)
    List<? super @StripTop Object> explicitTopSuperWildcard() {
        return null;
    }
}
