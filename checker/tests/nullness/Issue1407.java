// Test case for Issue #1407
// https://github.com/eisop/checker-framework/issues/1407

// @skip-test

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
            // false positive: should not raise an error here if we can substitute the condition
            // with the expression it's assigned to
            obj.toString();
        }
    }
}
