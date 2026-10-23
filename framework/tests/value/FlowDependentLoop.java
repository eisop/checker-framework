import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.PolyValue;

// The type of a method invocation or a conditional expression depends on the refined types of its
// subexpressions, so it must not be cached while the dataflow analysis is still iterating over a
// loop (typetools#602).
public class FlowDependentLoop {
    @PolyValue int id(@PolyValue int x) {
        return x;
    }

    boolean c() {
        return true;
    }

    void polyInLoop() {
        int i = 0;
        while (c()) {
            // :: error: (assignment.type.incompatible)
            @IntVal(0) int j = id(i);
            i = 1;
        }
    }

    void conditionalOfInvocationInLoop(boolean b) {
        int i = 0;
        while (c()) {
            // :: error: (assignment.type.incompatible)
            @IntRange(from = 0, to = 1) int j = b ? id(i) : 1;
            i = 5;
        }
    }

    static <T> T bar() {
        throw new Error();
    }

    static <T> @PolyValue Object first(@PolyValue T a, T b) {
        return a;
    }

    static @PolyValue Integer boxedId(@PolyValue Integer x) {
        return x;
    }

    // Inferring the type arguments of bar() also types the later argument boxedId(i), whose node
    // has not been visited yet in the current pass over the loop.
    void inferredWithLaterArgument() {
        Integer i = 0;
        while (c()) {
            // :: error: (assignment.type.incompatible)
            @IntVal(0) Object o = first(bar(), boxedId(i));
            i = 1;
        }
    }
}
