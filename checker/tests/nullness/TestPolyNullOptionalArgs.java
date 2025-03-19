import org.checkerframework.checker.nullness.qual.*;

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
        // :: error: (assignment.type.incompatible)
        arr1 = identity(arr2);
        arr2 = identity(arr2);
        arr3 = identity(arr3);
        arr4 = identity(arr4);
    }
}
