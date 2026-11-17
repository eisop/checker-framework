import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.List;

// The element of an enhanced for loop keeps its qualifier when it is unboxed, and possibly
// widened, for a primitive loop variable.
public class EnhancedForUnboxing {
    void iterable(List<@Untainted Integer> list) {
        for (int x : list) {
            @Untainted int y = x;
        }
        for (long x : list) {
            @Untainted long y = x;
        }
    }

    void array(@Untainted Integer[] array) {
        for (int x : array) {
            @Untainted int y = x;
        }
        for (long x : array) {
            @Untainted long y = x;
        }
    }

    void tainted(List<Integer> list) {
        for (int x : list) {
            // :: error: (assignment.type.incompatible)
            @Untainted int y = x;
        }
    }
}
