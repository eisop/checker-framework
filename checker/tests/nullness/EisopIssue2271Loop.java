// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2271 : a diamond in a loop.
// An anonymous diamond behaves as a named diamond does.

import org.checkerframework.checker.nullness.qual.Nullable;

class EisopIssue2271Loop {
    static class Cell<T extends @Nullable Object> {
        Cell(T t) {}

        T get() {
            throw new RuntimeException();
        }
    }

    static boolean c() {
        return true;
    }

    // Known missed error, for both the named and the anonymous diamond (eisop#2343): `n` is nullable in the
    // second iteration, so `get()` should return a nullable value. The type arguments of the
    // diamond are not inferred again after the loop's store changes.
    void namedInLoop() {
        @Nullable Object n = "";
        while (c()) {
            new Cell<>(n).get().toString();
            n = null;
        }
    }

    void anonymousInLoop() {
        @Nullable Object n = "";
        while (c()) {
            new Cell<>(n) {}.get().toString();
            n = null;
        }
    }

    void named(@Nullable Object n) {
        // :: error: (dereference.of.nullable)
        new Cell<>(n).get().toString();
    }

    void anonymous(@Nullable Object n) {
        // :: error: (dereference.of.nullable)
        new Cell<>(n) {}.get().toString();
    }
}
