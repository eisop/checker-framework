// Test that a stub-file fake override binds to the overload whose parameter types match
// exactly, not to a type-variable overload that happens to be visited first.
// The fake override is in tests/nullness-stubfile/fakeOverloadOverride.astub.

import org.checkerframework.checker.nullness.qual.NonNull;

public class FakeOverloadOverride {

    void use(FakeOverloadSub sub) {
        // fakeOverloadOverride.astub declares a fake override of f(String) on FakeOverloadSub
        // with a @Nullable return type.
        // :: error: (assignment.type.incompatible)
        @NonNull String s = sub.f("hello");
        // The type-variable overload is unaffected by the fake override.
        @NonNull Object o = sub.f(new Object());
        o.toString();
    }
}

class FakeOverloadSuper {
    // The type-variable overload is declared first, so that a matching pass without
    // exact-match preference would bind the fake override for f(String) to it.
    <T> T f(T x) {
        return x;
    }

    String f(String x) {
        return x;
    }
}

class FakeOverloadSub extends FakeOverloadSuper {}
