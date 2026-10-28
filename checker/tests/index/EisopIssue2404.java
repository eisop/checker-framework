// Test case for https://github.com/eisop/checker-framework/issues/2404 :
// the Index Checker crashed on an expression whose type is a type variable bounded by Integer,
// because it read the primary qualifier of the expression's type, which a type variable does not
// have; the qualifier is on its bound. The shapes with a comparison, a shift, or an addition also
// need the CFG builder to unbox the type variable through its bound (eisop#2332).

import java.util.Random;

public class EisopIssue2404 {
    static <N extends Integer> int access(int[] a, N x) {
        // :: error: (array.access.unsafe.low) :: error: (array.access.unsafe.high)
        return a[x];
    }

    // TODO: the comparisons do not yet refine a type-variable-typed index.
    static <N extends Integer> int guarded(int[] a, N x) {
        if (x >= 0 && x < a.length) {
            // :: error: (array.access.unsafe.low) :: error: (array.access.unsafe.high)
            return a[x];
        }
        return 0;
    }

    static <N extends Integer> int min(N x, int y) {
        return Math.min(x, y);
    }

    static <N extends Integer> int max(N x, int y) {
        return Math.max(x, y);
    }

    static <N extends Integer> int nextInt(Random r, N x) {
        // :: error: (argument.type.incompatible)
        return r.nextInt(x);
    }

    static <N extends Integer> int shift(N x) {
        return x >> 2;
    }

    static <N extends Integer> int plusOne(int[] a, N x) {
        if (x < a.length - 1) {
            // :: error: (array.access.unsafe.low)
            return a[x + 1];
        }
        return 0;
    }
}
