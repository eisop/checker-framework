import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.PolyValue;

// The value of a postfix increment or decrement is the old value of its operand. While dataflow
// analysis types the invocation that uses it, the type of the postfix expression must be the type
// of that value, and not the declared type, which would then be cached for the invocation.
public class PostfixPoly {
    static @PolyValue Integer bid(@PolyValue Integer x) {
        return x;
    }

    boolean c() {
        return true;
    }

    void increment() {
        int i = 0;
        @IntVal(0) Object o = bid(i++);
    }

    void decrement() {
        int i = 0;
        @IntVal(0) Object o = bid(i--);
    }

    void afterwards() {
        int i = 0;
        @IntVal(0) Object o = bid(i++);
        // :: error: (assignment.type.incompatible)
        @IntVal(0) Object p = bid(i++);
    }

    void inLoop() {
        int i = 0;
        while (c()) {
            // :: error: (assignment.type.incompatible)
            @IntVal(0) Object o = bid(i++);
        }
    }
}
