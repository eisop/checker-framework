import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Searching through nullable array components
// and for nullable keys is forbidden.
public class BinarySearch {
    @Nullable Object @NonNull [] arr = {"a", "b", null};

    void search(@Nullable Object key) {
        // :: error: (argument.type.incompatible)
        int res = java.util.Arrays.binarySearch(arr, key);
        // :: error: (argument.type.incompatible)
        res = java.util.Arrays.binarySearch(arr, 0, 4, key);
    }
}
