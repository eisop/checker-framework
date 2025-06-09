// Test case for Issue 412:
// https://github.com/eisop/checker-framework/issues/412

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Issue412 {
    class InnerWithUnknownInitializationEnclosingExpression {
        InnerWithUnknownInitializationEnclosingExpression(
                @UnknownInitialization Issue412 Issue412.this) {
            // @UnknownInitialization indicates that the field of the outer receiver might be null
            // :: error : (dereference.of.nullable)
            Issue412.this.f.hashCode();
        }
    }

    Issue412() {
        new InnerWithUnknownInitializationEnclosingExpression();
        f = "";
    }

    Object f;
}
