import org.checkerframework.framework.testchecker.striplocation.quals.StripBottom;
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

    // @StripBottom has no @TargetLocations of its own, so writing it explicitly on a lower-bound
    // location is not something this checker's @TargetLocations-based mechanism can flag; only the
    // opt-in checker's own additional, tree-based rule (see StripLocationValidator) reports it, so
    // with the opt-in off there is no error here.
    static class ExplicitBottomParam<@StripBottom T extends Object> {}

    // Relying on defaulting for the same lower-bound position; never an error either way.
    static class DefaultedBottomParam<T extends Object> {}

    // Same two cases for a wildcard's super (lower) bound.
    List<@StripBottom ? extends Object> explicitBottomWildcard() {
        return null;
    }

    List<? extends Object> defaultedBottomWildcard() {
        return null;
    }
}
