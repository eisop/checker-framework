import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.PolyValue;

// A finally block is copied into the control flow graph once per way of leaving the try block, so
// one tree has several nodes. The cached type of a tree must not be served from the node of
// another copy or of an earlier pass over a loop (see FlowDependentLoop).
public class FlowDependentFinally {
    static @PolyValue Integer boxedId(@PolyValue Integer x) {
        return x;
    }

    static <T> T bar() {
        throw new Error();
    }

    static <T> @PolyValue Object first(@PolyValue T a, T b) {
        return a;
    }

    boolean c() {
        return true;
    }

    void tryFinallyLoop() {
        Integer i = 0;
        while (c()) {
            try {
                if (c()) {
                    continue;
                }
                if (c()) {
                    return;
                }
            } finally {
                // :: error: (assignment.type.incompatible)
                @IntVal(0) Object o = first(bar(), boxedId(i));
                i = 1;
            }
        }
    }
}
