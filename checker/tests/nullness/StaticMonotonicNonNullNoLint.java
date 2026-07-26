import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

// Test for issue #949: the monotonic.on.static warning is opt-in. Without
// -Alint=monotonicNonNullOnStatic (the default, as used by this test directory), a static
// @MonotonicNonNull field produces NO warning.
public class StaticMonotonicNonNullNoLint {

    // No warning here: the lint option is not enabled.
    static @MonotonicNonNull Object staticField;

    @MonotonicNonNull Object instanceField;
}
