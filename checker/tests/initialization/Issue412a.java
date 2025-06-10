// Test case for Issue 412:
// https://github.com/eisop/checker-framework/issues/412

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Issue412a {
    class InnerWithUnknownInitializationEnclosingExpression {
        InnerWithUnknownInitializationEnclosingExpression(
                @UnknownInitialization Issue412a Issue412a.this) {
            // @UnknownInitialization indicates that the field of the outer receiver might be null
            // :: error : (dereference.of.nullable)
            Issue412a.this.f.hashCode();
        }
    }

    Issue412a() {
        new InnerWithUnknownInitializationEnclosingExpression();
        f = "";
    }

    Object f;
}
