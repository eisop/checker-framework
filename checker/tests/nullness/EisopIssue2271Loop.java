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

    // `n` is nullable in the second iteration, so `get()` returns a nullable value (eisop#2343).
    void namedInLoop() {
        @Nullable Object n = "";
        while (c()) {
            // :: error: (dereference.of.nullable)
            new Cell<>(n).get().toString();
            n = null;
        }
    }

    void anonymousInLoop() {
        @Nullable Object n = "";
        while (c()) {
            // :: error: (dereference.of.nullable)
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
