// Test case for eisop#2655: a poly conditional expression in an invocation context. javac gives
// it the captured target type, Comparator<capture of ? super T>, which neither operand is a
// subtype of. Its operands are checked against the target type by the argument check, not against
// the conditional's own type, so the call type-checks without a conditional.type.incompatible
// error and without a crash in StructuralEqualityComparer.

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
}
