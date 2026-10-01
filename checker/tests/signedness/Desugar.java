import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;

import java.util.Map;

public class Desugar {

    void test(int x) {
        int i = getI();
        Integer box = i;
        @Signed Integer boxy = box;
        @Signed Integer box2 = method(box);
    }

    void testDesugared(int x) {
        int i = getI();
        Integer box = Integer.valueOf(i);
        @Signed Integer boxy = box;
        @Signed Integer box2 = method(box);
    }

    // A loop makes the boxing node be analyzed more than once, so the node for the valueOf call
    // already has a value from the previous iteration when the argument tree is queried again.
    void testBoxingInLoop(boolean c) {
        int i = getI();
        while (c) {
            Integer box = i;
            @Signed Integer boxy = box;
            @Signed Integer box2 = method(box);
        }
    }

    // Regression coverage for boxing alongside a compound assignment, which evaluates its target
    // tree more than once within a single pass.  This case passes either way; it is here so that
    // the combination stays covered.
    void testRepeatedEvaluation(byte b) {
        b |= 1;
        Integer box = getI();
        @Signed Integer boxy = box;
    }

    @PolySigned Integer method(@PolySigned Integer i) {
        return i;
    }

    @Signed int getI() {
        return 0;
    }

    void test2(Map<Integer, String> nonceMap, String nextInvo) {
        int invoNonce = calcNonce(nextInvo);
        Integer key = invoNonce;
        String enterInvo = nonceMap.get(key);
    }

    private @Signed int calcNonce(String invocation) {
        return 0;
    }
}
