// Test case for EISOP issue 610:
// https://github.com/eisop/checker-framework/issues/610

// The issue was that receivers of class type `A` whose fields are all initialized were sometimes
// considered `@UnderInitialization(A.class)` instead of `@Initialized` even when `A` was final.

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class EisopIssue610_1 {
    @MonotonicNonNull String s;

    EisopIssue610_1() {
        init();
    }

    void init() {}
}

final class EisopIssue610_2 {
    @Nullable String s;

    EisopIssue610_2() {
        init();
    }

    void init() {}
}

final class EisopIssue610_3 {
    @MonotonicNonNull String s;

    EisopIssue610_3() {
        @Initialized EisopIssue610_3 other = this;
    }
}

final class EisopIssue610_4 {
    @Nullable String s;

    EisopIssue610_4() {
        @Initialized EisopIssue610_4 other = this;
    }
}
