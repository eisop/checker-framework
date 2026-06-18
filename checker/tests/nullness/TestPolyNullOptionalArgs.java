import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.List;

public class TestPolyNullOptionalArgs {

    public static @PolyNull("elt") Object @PolyNull("container") [] identity(
            @PolyNull("elt") Object @PolyNull("container") [] seq) {
        return seq;
    }

    void testArray(
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

    @SuppressWarnings("return.type.incompatible")
    public static <E> @PolyNull("elt") Object @PolyNull("container") [] toArray(
            @PolyNull("container") List<@PolyNull("elt") E> list) {
        return null;
    }

    void testList(
            List<@Nullable Object> list1,
            @Nullable List<@Nullable Object> list2,
            List<Object> list3,
            @Nullable List<Object> list4,
            @Nullable Object[] arr1,
            @Nullable Object @Nullable [] arr2,
            Object[] arr3,
            Object @Nullable [] arr4) {
        arr1 = toArray(list1);
        // lhs: @Nullable Object @Nonnull []; rhs input: @Nullable List<@Nullable Object>
        // :: error: (assignment.type.incompatible)
        arr1 = toArray(list2);
        // lhs: @Nullable Object @Nonnull []; rhs input: @Nonnull List<@Nonnull Object>
        arr1 = toArray(list3);
        // lhs: @Nullable Object @Nonnull []; rhs input: @Nullable List<@Nonnull Object>
        // :: error: (assignment.type.incompatible)
        arr1 = toArray(list4);
        // lhs: @Nullable Object @Nullable []; rhs input: @Nullable List<@Nullable Object>
        arr2 = toArray(list2);
        // lhs: @Nullable Object @Nullable []; rhs input: @Nonnull List<@Nullable Object>
        arr2 = toArray(list1);
        // lhs: @Nullable Object @Nullable []; rhs input: @Nonnull List<@Nonnull Object>
        arr2 = toArray(list3);
        // lhs: @Nullable Object @Nullable []; rhs input: @Nullable List<@Nonnull Object>
        arr2 = toArray(list4);
        arr3 = toArray(list3);
        // lhs: @Nonnull Object @Nonnull []; rhs input: @Nonnull List<@Nullable Object>
        // :: error: (assignment.type.incompatible)
        arr3 = toArray(list1);
        // lhs: @Nonnull Object @Nonnull []; rhs input: @Nullable List<@Nullable Object>
        // :: error: (assignment.type.incompatible)
        arr3 = toArray(list2);
        // lhs: @Nonnull Object @Nonnull []; rhs input: @Nullable List<@Nonnull Object>
        // :: error: (assignment.type.incompatible)
        arr3 = toArray(list4);
        arr4 = toArray(list4);
        // lhs: @Nonnull Object @Nullable []; rhs input: @Nonnull List<@Nullable Object>
        // :: error: (assignment.type.incompatible)
        arr4 = toArray(list1);
        // lhs: @Nonnull Object @Nullable []; rhs input: @Nullable List<@Nullable Object>
        // :: error: (assignment.type.incompatible)
        arr4 = toArray(list2);
        arr4 = toArray(list3);
    }
}
