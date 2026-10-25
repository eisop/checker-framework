// Test that a dependent type annotation on a varargs parameter is viewpoint-adapted at a call that
// passes the varargs elements individually.

import org.checkerframework.checker.index.qual.IndexFor;

public class IndexForVarargsElements {
    static void indexes(int[] a, @IndexFor("#1") int... is) {}

    void caller(int[] a, int[] b, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        indexes(a);
        indexes(a, i);
        indexes(a, i, j);

        // :: error: (argument.type.incompatible)
        indexes(b, i);
    }
}
