// Test case for issue 104: https://github.com/eisop/checker-framework/issues/104

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ReceiverTypeArgs {
    static class Box<T> {
        T item;

        public Box(T item) {
            this.item = item;
        }

        void test(Box<@NonNull T> this) {}
    }

    private static void foo(Box<@Nullable String> box) {
        // :: error: (method.invocation.invalid)
        box.test();
    }
}
