/*
 * @test
 * @summary Test the monotonicNonNullOnStatic lint option: without -Alint=monotonicNonNullOnStatic
 *          (the default), a static MonotonicNonNull field produces no warning.
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker MonotonicNonNullOnStaticDisabled.java
 */

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MonotonicNonNullOnStaticDisabled {

    // No warning here: the lint option is not enabled.
    static @MonotonicNonNull Object staticField;

    @MonotonicNonNull Object instanceField;
}
