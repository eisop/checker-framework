// Test that a dependent type annotation on a varargs parameter is viewpoint-adapted at a call that
// passes the varargs elements individually.

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.SameLen;

public class IndexForVarargsElements {
    static void indexes(int[] a, @IndexFor("#1") int... is) {}

    static void sameLen(String[] a, String @SameLen("#1") ... bs) {}

    void caller(int[] a, int[] b, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        indexes(a);
        indexes(a, i);
        indexes(a, i, j);

        // :: error: (argument.type.incompatible)
        indexes(b, i);
    }

    void arrayAnnotation(String[] a, String x, String y) {
        // The annotation on the varargs array must not be copied to the elements, so the only
        // error is about the array.
        // TODO: false positive; the implicitly created array is not known to have a's length.
        // :: error: (varargs.type.incompatible)
        sameLen(a, x, y);
    }
}
