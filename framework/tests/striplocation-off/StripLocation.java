import org.checkerframework.framework.testchecker.striplocation.quals.StripBottom;
import org.checkerframework.framework.testchecker.striplocation.quals.StripTop;
import org.checkerframework.framework.testchecker.striplocation.quals.StripUpperOnly;

import java.util.List;

// Default behavior (opt-in off): a qualifier written at a location its @TargetLocations forbids is
// reported, but it still takes effect and produces a bound.type.incompatible cascade.
public class StripLocation {

    // Valid use: @StripUpperOnly is permitted on an upper bound, so there is no error.
    static class ValidUpper<T extends @StripUpperOnly Object> {}

    // @StripUpperOnly on a type-parameter (lower-bound) location is invalid, and because it is
    // above
    // @StripBottom in the hierarchy it also makes the bounds incompatible.
    // :: error: (type.invalid.annotations.on.location)
    // :: error: (bound.type.incompatible)
    static class BadParam<@StripUpperOnly T extends @StripBottom Object> {}

    // @StripUpperOnly on a wildcard super (lower) bound location is invalid, and it likewise makes
    // the wildcard bounds incompatible.
    // :: error: (type.invalid.annotations.on.location)
    // :: error: (bound.type.incompatible)
    List<@StripUpperOnly ? extends @StripBottom Object> badWildcard() {
        return null;
    }

    // @StripTop has no @TargetLocations of its own, so writing it explicitly on a lower-bound
    // location is not something this checker's @TargetLocations-based mechanism can flag; only the
    // opt-in checker's own additional, tree-based rule (see StripLocationValidator) reports it. But
    // an explicit @StripTop lower bound is incompatible with the @StripBottom upper bound
    // regardless, so with the opt-in off this default behavior still reports a cascade.
    // :: error: (bound.type.incompatible)
    static class ExplicitTopParam<@StripTop T extends @StripBottom Object> {}

    // Relying on defaulting for the same lower-bound position; never an error either way.
    static class DefaultedTopParam<T extends @StripBottom Object> {}

    // Same two cases for a wildcard's super (lower) bound.
    // :: error: (bound.type.incompatible)
    List<@StripTop ? extends @StripBottom Object> explicitTopWildcard() {
        return null;
    }

    List<? extends @StripBottom Object> defaultedTopWildcard() {
        return null;
    }

    // The ? super form: the explicit lower bound is written after "super" rather than as a primary
    // annotation on "?". No incompatible-bound scenario is expressible here (an explicit @StripTop
    // super bound is always compatible with the implicit, defaulted-to-top extends bound), so this
    // exercises only the ? super tree shape, not the cascade.
    List<? super @StripTop Object> explicitTopSuperWildcard() {
        return null;
    }
}
