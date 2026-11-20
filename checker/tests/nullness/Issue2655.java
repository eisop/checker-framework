// Test case for eisop#2655: the operands of a poly conditional expression are checked against the
// parameter type, not against the conditional's captured type Comparator<capture of ? super T>.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Comparator;

class Issue2655 {
    static <T extends @Nullable Object> Comparator<@Nullable T> wrap(
            Comparator<? super T> delegate) {
        throw new UnsupportedOperationException();
    }

    static <T extends @Nullable Object> Comparator<@Nullable T> chain(
            @Nullable Comparator<T> first, Comparator<? super @Nullable T> second) {
        return wrap(first == null ? second : first.thenComparing(second));
    }

    static void takeNonNull(Object o) {}

    static void stillChecked(boolean b) {
        // :: error: (argument.type.incompatible)
        takeNonNull(b ? null : "");
    }
}
