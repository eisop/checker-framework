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

    void arrayOfSameLength(String[] a, String @SameLen("#1") [] b) {
        // The annotation on the varargs array is still enforced on an explicit array.
        sameLen(a, b);
    }

    void arrayOfOtherLength(String[] a, String[] c) {
        // :: error: (argument.type.incompatible)
        sameLen(a, c);
    }

    void arrayAnnotation(String[] a, String x, String y) {
        // The annotation on the varargs array must not be copied to the elements, so the only
        // error is about the array: the implicitly created array has length 2, which is not
        // known to be the length of a.
        // :: error: (varargs.type.incompatible)
        sameLen(a, x, y);
    }
}
