// Test case for Issue #1407
// https://github.com/eisop/checker-framework/issues/1407

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1407 {
    void inlinedCondition(@Nullable Object obj) {
        if (obj != null) {
            obj.toString();
        }
    }

    void wrappedCondition(@Nullable Object obj) {
        boolean cond = obj != null;
        if (cond) {
            // :: error: (dereference.of.nullable)
            obj.toString();
        }
    }
}
