// A type argument outside the bounds of its type parameter is reported, but leaves the type valid,
// so the checks that compare the type still run.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeArgumentOutOfBoundsStillChecked {
    static class Foo<T extends @NonNull Object> {}

    // :: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
    Foo<@Nullable Object> field = null;

    // :: error: (type.argument.type.incompatible)
    Foo<@Nullable Object> method() {
        // :: error: (return.type.incompatible)
        return null;
    }

    void local() {
        // :: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
        @NonNull Foo<@Nullable Object> f = null;
    }
}
