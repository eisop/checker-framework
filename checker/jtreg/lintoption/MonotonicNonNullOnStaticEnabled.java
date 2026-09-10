/*
 * @test
 * @summary Test the monotonicNonNullOnStatic lint option: with -Alint=monotonicNonNullOnStatic, a
 *          static MonotonicNonNull field warns, a non-static one does not, and the warning is
 *          suppressible by the message key and by the checker name.
 * @compile/ref=MonotonicNonNullOnStaticEnabled.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint=monotonicNonNullOnStatic MonotonicNonNullOnStaticEnabled.java
 */

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class MonotonicNonNullOnStaticEnabled {

    // A static @MonotonicNonNull field is a code smell and triggers a warning.
    static @MonotonicNonNull Object staticField;

    // A non-static @MonotonicNonNull field does NOT trigger the warning.
    @MonotonicNonNull Object instanceField;

    // The warning is suppressible via the message key.
    @SuppressWarnings("monotonic.on.static")
    static @MonotonicNonNull Object suppressedByKey;

    // It is also suppressible via the checker name.
    @SuppressWarnings("nullness")
    static @MonotonicNonNull Object suppressedByChecker;
}
