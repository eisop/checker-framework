import org.checkerframework.common.value.qual.MinLen;

// The type of a method invocation depends on the refined types of its arguments, so it must not be
// cached while the dataflow analysis is still iterating over a loop (typetools#602).
public class FlowDependentLoop {
    boolean c() {
        return true;
    }

    void valueOfInLoop(int @MinLen(1) [] a, int n) {
        int i = 0;
        while (c()) {
            // :: error: (array.access.unsafe.high) :: error: (array.access.unsafe.low)
            a[Integer.valueOf(i)] = 0;
            i = n;
        }
    }

    void literalThenLength(int @MinLen(1) [] a) {
        int i = 0;
        while (c()) {
            // :: error: (array.access.unsafe.high)
            a[Integer.valueOf(i)] = 0;
            i = a.length;
        }
    }
}
