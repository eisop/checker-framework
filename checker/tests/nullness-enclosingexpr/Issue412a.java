// Test case for Issue 412:
// https://github.com/eisop/checker-framework/issues/412

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class Issue412a {
    class InnerWithUnknownInitializationEnclosingExpression {
        InnerWithUnknownInitializationEnclosingExpression(
                @UnknownInitialization Issue412a Issue412a.this) {
            // The explicit annotation in signature should be captured, hence raise an error during
            // the check of method body
            // :: error: (dereference.of.nullable)
            Issue412a.this.f.hashCode();
        }
    }

    class InnerWithoutUnknownInitializationEnclosingExpression {
        InnerWithoutUnknownInitializationEnclosingExpression() {
            Issue412a.this.f.hashCode();
        }
    }

    Issue412a() {
        new InnerWithUnknownInitializationEnclosingExpression();
        // If not explicit specified, the error should be raised at call site
        // :: error: (enclosingexpr.type.incompatible)
        new InnerWithoutUnknownInitializationEnclosingExpression();
        f = "";
    }

    Object f;
}
