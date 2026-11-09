import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java17-jdk-skip-test
// The result of a switch expression is converted to the type of the switch expression; here that
// unboxes a nullable Integer, so the value is non-null afterwards.
public class UnboxingSwitchExpression {

    void arrowResult(@Nullable Integer i, int k) {
        // Every result unboxes i, so i is non-null after the switch expression.
        int x =
                switch (k) {
                    // :: error: (unboxing.of.nullable)
                    case 0 -> i;
                    // :: error: (unboxing.of.nullable)
                    default -> i;
                };
        i.toString();
    }

    void yieldResult(@Nullable Integer i, int k) {
        int x =
                switch (k) {
                    case 0:
                        // :: error: (unboxing.of.nullable)
                        yield i;
                    default:
                        // :: error: (unboxing.of.nullable)
                        yield i;
                };
        i.toString();
    }

    void boxedResult(int k) {
        Integer x =
                switch (k) {
                    case 0 -> 0;
                    default -> 1;
                };
        x.toString();
    }
}
