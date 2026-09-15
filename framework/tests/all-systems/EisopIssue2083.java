// Test case for EISOP Issue 2083:
// https://github.com/eisop/checker-framework/issues/2083

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EisopIssue2083 {
    void primitiveArray(List<? extends int[]> in) {
        new ArrayList<>(in).forEach((int[] b) -> {});
    }

    void referenceArray(List<? extends String[]> in) {
        new ArrayList<>(in).forEach((String[] b) -> {});
    }

    static <Z> void each(List<? extends Z> a, List<? extends Z> b, Consumer<Z> c) {}

    void twoLowerBounds(List<int[]> a, List<? extends int[]> l) {
        each(a, l, b -> {});
    }
}
