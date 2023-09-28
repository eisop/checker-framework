// Test case for eisop issue #297:
// https://github.com/eisop/checker-framework/issues/297
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class UnboxUninitalizedFieldTest {
    @UnknownInitialization Integer n;

    UnboxUninitalizedFieldTest() {
        // :: error: (unboxing.of.nullable)
        int y = n;
    }
}
