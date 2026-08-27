// Test case for casts of the null literal, which have no elements to check.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class NullLiteralCast {

    @Nullable Object @Nullable [] toArray() {
        return (@Nullable Object[]) null;
    }

    int @Nullable [] toPrimitiveArray() {
        return (int[]) null;
    }

    @Nullable Object @Nullable [] @Nullable [] toNestedArray() {
        return (@Nullable Object[][]) null;
    }

    @Nullable List<@Nullable String> toParameterizedType() {
        return (List<@Nullable String>) null;
    }

    void toVarargs() {
        // :: error: (argument.type.incompatible)
        varargs((@Nullable Object[]) null);
    }

    void varargs(@Nullable Object... args) {}

    // A cast to an array type from a non-null expression is still not statically verifiable.
    @SuppressWarnings("unchecked") // The cast is an unchecked cast.
    @Nullable Object[] fromObject(Object o) {
        // :: warning: (cast.unsafe)
        return (@Nullable Object[]) o;
    }
}
