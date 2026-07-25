import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

// Test for issue #949: warn when @MonotonicNonNull is written on a static field.
public class StaticMonotonicNonNull {

    // A static @MonotonicNonNull field is a code smell and triggers a warning.
    // :: warning: (monotonic.on.static)
    static @MonotonicNonNull Object staticField;

    // A non-static @MonotonicNonNull field does NOT trigger the warning.
    @MonotonicNonNull Object instanceField;

    // The warning is suppressible via the standard @SuppressWarnings mechanism.
    @SuppressWarnings("monotonic.on.static")
    static @MonotonicNonNull Object suppressedByKey;

    // It is also suppressible via the checker name.
    @SuppressWarnings("nullness")
    static @MonotonicNonNull Object suppressedByChecker;
}
