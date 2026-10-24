import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;

import java.io.StringReader;
import java.util.List;
import java.util.function.Consumer;

// A compound assignment, increment, or decrement creates artificial trees in the CFG (the
// operation and a cast). The dependent-type expressions in the type of such a tree must be
// resolved in the scope of the expression it was created for, which includes the local variables
// declared before it. Except for the one marked, none of the operations below is an error.
public class ArtificialTreeScope {

    int[] field = new int[10];

    void indexOrHigh(@NonNegative int n) {
        int[] b = new int[n];
        @IndexOrHigh("b") int j = b.length;
        j -= 1;
    }

    void indexOrHighInBlock(@NonNegative int n) {
        if (n > 0) {
            int[] b = new int[n];
            @IndexOrHigh("b") int j = b.length;
            j -= 1;
        }
    }

    void compoundAssignments(@NonNegative int n) {
        int[] b = new int[n];
        @LTEqLengthOf("b") int j = b.length;
        j -= 1;
        j = b.length;
        j += 0;
        j = b.length;
        // The Upper Bound Checker does not know that multiplying by 1 is the identity.
        // :: error: (compound.assignment.type.incompatible)
        j *= 1;
        j = b.length;
        j /= 2;
        j = b.length;
        j %= 1;
        j = b.length;
        j <<= 0;
        j = b.length;
        j >>= 1;
        j = b.length;
        j >>>= 1;
        j = b.length;
        j &= 1;
        j = b.length;
        j |= 0;
        j = b.length;
        j ^= 0;
    }

    void incrementsAndDecrements(@NonNegative int n) {
        int[] b = new int[n];
        @LTEqLengthOf("b") int j = b.length;
        j--;
        j = b.length;
        --j;
        j = b.length;
        int unused1 = j--;
        j = b.length;
        int unused2 = --j;
        @LTEqLengthOf("b") int k = b.length - 1;
        k++;
        k = b.length - 1;
        ++k;
        k = b.length - 1;
        int unused3 = k++;
        k = b.length - 1;
        int unused4 = ++k;
    }

    void parameter(int[] b) {
        @LTEqLengthOf("b") int j = b.length;
        j -= 1;
        j--;
    }

    void field() {
        @LTEqLengthOf("field") int j = field.length;
        j -= 1;
        j--;
        @LTEqLengthOf("this.field") int k = this.field.length;
        k -= 1;
        k--;
    }

    void nestedBlocks(@NonNegative int n) {
        if (n > 0) {
            int[] b = new int[n];
            {
                int[] c = b;
                @LTEqLengthOf({"b", "c"}) int j = c.length;
                j -= 1;
                j--;
            }
        }
    }

    void loops(@NonNegative int n, List<String> list) {
        for (int i = 0; i < n; i++) {
            int[] b = new int[n];
            @LTEqLengthOf("b") int j = b.length;
            j -= 1;
        }
        while (n > 0) {
            int[] b = new int[n];
            @LTEqLengthOf("b") int j = b.length;
            j -= 1;
            n--;
        }
        do {
            int[] b = new int[n];
            @LTEqLengthOf("b") int j = b.length;
            j >>= 1;
        } while (n > 1);
        for (String s : list) {
            int[] b = new int[n];
            @LTEqLengthOf("b") int j = b.length;
            j -= 1;
        }
        for (int[] b : new int[][] {new int[n]}) {
            @LTEqLengthOf("b") int j = b.length;
            j -= 1;
        }
    }

    void switchStatement(@NonNegative int n, int s) {
        switch (s) {
            case 0:
                int[] b = new int[n];
                @LTEqLengthOf("b") int j = b.length;
                j -= 1;
                break;
            default:
                int[] c = new int[n];
                @LTEqLengthOf("c") int k = c.length;
                k--;
        }
    }

    void tryWithResources(@NonNegative int n) throws Exception {
        try (StringReader r = new StringReader("")) {
            int[] b = new int[n];
            @LTEqLengthOf("b") int j = b.length;
            j -= 1;
        }
    }

    void conditional(@NonNegative int n, boolean c) {
        int[] b = new int[n];
        @LTEqLengthOf("b") int j = b.length;
        @LTEqLengthOf("b") int k = c ? (j -= 1) : (j >>= 1);
    }

    void lambda(@NonNegative int n) {
        int[] b = new int[n];
        Runnable r =
                () -> {
                    @LTEqLengthOf("b") int j = b.length;
                    j -= 1;
                    j--;
                    int[] d = new int[n];
                    @LTEqLengthOf("d") int k = d.length;
                    k -= 1;
                };
    }

    void lambdaWithParameter() {
        Consumer<int[]> c =
                (int[] a) -> {
                    // A dependent type that names the lambda parameter, or a variable whose type
                    // refers to it, crashes with or without an artificial tree; see eisop#2355.
                    int[] b = new int[3];
                    @LTEqLengthOf("b") int j = b.length;
                    j -= 1;
                    j--;
                };
    }

    void anonymousClass(@NonNegative int n) {
        int[] b = new int[n];
        new Object() {
            void m() {
                @LTEqLengthOf("b") int j = b.length;
                j -= 1;
                int[] d = new int[n];
                @LTEqLengthOf("d") int k = d.length;
                k--;
            }
        };
    }

    void localClass(@NonNegative int n) {
        int[] b = new int[n];
        class Local {
            void m() {
                @LTEqLengthOf("b") int j = b.length;
                j -= 1;
                int[] d = new int[n];
                @LTEqLengthOf("d") int k = d.length;
                k--;
            }
        }
    }

    void lessThan(@NonNegative int n) {
        int m = n + 1;
        @LessThan("m") int j = m - 1;
        j -= 1;
        j--;
    }

    void sameLen(@NonNegative int n) {
        int[] a = new int[n];
        @SameLen("a") int[] b = new int[a.length];
        @LTEqLengthOf({"a", "b"}) int j = b.length;
        j -= 1;
    }
}
