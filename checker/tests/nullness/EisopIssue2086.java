// Test case for EISOP Issue 2086:
// https://github.com/eisop/checker-framework/issues/2086
// Inference of arr(...) must still see the nullable component type of the new array.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Supplier;

public class EisopIssue2086 {

    static <R> R run(Supplier<R> s) {
        throw new Error();
    }

    static <T> T[] arr(T[] a) {
        return a;
    }

    @Nullable String[] ok() {
        return run(() -> arr(new @Nullable String[0]));
    }

    String[] bad() {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return run(() -> arr(new @Nullable String[0]));
    }
}
