/*
 * @test
 * @summary Test stripping location-invalid qualifiers from bounds (both with opt-in on and off).
 *
 * @compile/fail/ref=StripLocationOff.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.striplocation.StripLocationChecker StripLocationTest.java
 * @compile/fail/ref=StripLocationOn.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.striplocation.StripLocationChecker -AstripInvalidLocationQualifiers StripLocationTest.java
 */

import org.checkerframework.framework.testchecker.striplocation.quals.StripBottom;
import org.checkerframework.framework.testchecker.striplocation.quals.StripTop;
import org.checkerframework.framework.testchecker.striplocation.quals.StripUpperOnly;

import java.util.List;

public class StripLocationTest {

    // Valid use: @StripUpperOnly is permitted on an upper bound, so there is no error.
    static class ValidUpper<T extends @StripUpperOnly Object> {}

    // @StripUpperOnly on a type-parameter (lower-bound) location is invalid.
    static class BadParam<@StripUpperOnly T extends @StripBottom Object> {}

    // @StripUpperOnly on a wildcard super (lower) bound location is invalid.
    List<@StripUpperOnly ? extends @StripBottom Object> badWildcard() {
        return null;
    }

    // @StripTop has no @TargetLocations, but StripLocationValidator's tree-based check
    // forbids writing it explicitly on a lower bound when opted in.
    static class ExplicitTopParam<@StripTop T extends @StripBottom Object> {}

    static class DefaultedTopParam<T extends @StripBottom Object> {}

    List<@StripTop ? extends @StripBottom Object> explicitTopWildcard() {
        return null;
    }

    List<? extends @StripBottom Object> defaultedTopWildcard() {
        return null;
    }

    List<? super @StripTop Object> explicitTopSuperWildcard() {
        return null;
    }
}
