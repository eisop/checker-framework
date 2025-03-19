import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class TestPolyNullOptionalArgs {

    public static @PolyNull("elt") Object @PolyNull("container") [] identity(
            @PolyNull("elt") Object @PolyNull("container") [] seq) {
        return seq;
    }

    void test(
            @Nullable Object[] arr1,
            @Nullable Object @Nullable [] arr2,
            Object[] arr3,
            Object @Nullable [] arr4) {
        arr1 = identity(arr1);
        // lhs: @Nullable Object @Nonnull []; rhs: @Nullable Object @Nullable []
        // :: error: (assignment.type.incompatible)
        arr1 = identity(arr2);
        // lhs: @Nullable Object @Nonnull []; rhs: @Nonnull Object @Nonnull []
        arr1 = identity(arr3);
        // lhs: @Nullable Object @Nonnull []; rhs: @Nonnull Object @Nullable []
        // :: error: (assignment.type.incompatible)
        arr1 = identity(arr4);
        arr2 = identity(arr2);
        // lhs: @Nullable Object @Nullable []; rhs: @Nullable Object @Nonnull []
        arr2 = identity(arr1);
        // lhs: @Nullable Object @Nullable []; rhs: @Nonnull Object @Nonnull []
        arr2 = identity(arr3);
        // lhs: @Nullable Object @Nullable []; rhs: @Nonnull Object @Nullable []
        arr2 = identity(arr4);
        arr3 = identity(arr3);
        // lhs: @Nonnull Object @Nonnull []; rhs: @Nullable Object @Nonnull []
        // :: error: (assignment.type.incompatible)
        arr3 = identity(arr1);
        // lhs: @Nonnull Object @Nonnull []; rhs: @Nullable Object @Nullable []
        // :: error: (assignment.type.incompatible)
        arr3 = identity(arr2);
        // lhs: @Nonnull Object @Nonnull []; rhs: @Nonnull Object @Nullable []
        // :: error: (assignment.type.incompatible)
        arr3 = identity(arr4);
        arr4 = identity(arr4);
        // lhs: @Nonnull Object @Nullable []; rhs: @Nullable Object @Nonnull []
        // :: error: (assignment.type.incompatible)
        arr4 = identity(arr1);
        // lhs: @Nonnull Object @Nullable []; rhs: @Nullable Object @Nullable []
        // :: error: (assignment.type.incompatible)
        arr4 = identity(arr2);
        arr4 = identity(arr3);
    }
}
