/*
 * @test
 * @summary Test that a type factory does not use a visitor tree path from the previous
 *     compilation unit. With caching turned off, such a path made the checker crash on "this"
 *     below.
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -AatfDoNotCache FirstUnit.java StaleVisitorPath.java
 */

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class StaleVisitorPath {
    @UnknownInitialization StaleVisitorPath f;

    StaleVisitorPath() {
        f = this;
    }
}
