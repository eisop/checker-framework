import org.checkerframework.checker.interning.qual.InternedDistinct;

// Regression test for the Java 8 type-argument-inference work budget
// (Java8InferenceContext.MAX_INCORPORATION_WORK).  Incorporating bounds to a fixed point is
// roughly cubic in the nesting depth of a generic invocation, so a deeply enough nested call would
// otherwise make inference take many seconds.  The budget abandons such an invocation and reports
// `type.argument.inference.budget` (a deliberate give-up, not a crash) telling the user to supply
// explicit type arguments.  The nesting depth below is chosen to comfortably exceed the budget; if
// the budget or the per-iteration cost changes, deepen or shorten the chain to match.
//
// Filed under the Interning Checker only, but the mechanism is in the framework and applies to
// every type system.  (Do not add @SuppressWarnings("interning") here: that is the checker name and
// would suppress the framework error under test.)
public class InferenceWorkBudget {

    static <T> T id(T x) {
        return x;
    }

    String tooDeeplyNested(String x) {
        // :: error: (type.argument.inference.budget)
        return id(
                id(
                        id(
                                id(
                                        id(
                                                id(
                                                        id(
                                                                id(
                                                                        id(
                                                                                id(
                                                                                        id(
                                                                                                id(
                                                                                                        id(
                                                                                                                id(
                                                                                                                        id(
                                                                                                                                id(
                                                                                                                                        id(
                                                                                                                                                id(
                                                                                                                                                        id(
                                                                                                                                                                id(
                                                                                                                                                                        id(
                                                                                                                                                                                id(
                                                                                                                                                                                        id(
                                                                                                                                                                                                id(
                                                                                                                                                                                                        id(
                                                                                                                                                                                                                id(
                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                id(
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        x))))))))))))))))))))))))))))))))))))))))))))))))))))))))))));
    }

    // A shallow generic invocation still infers normally and is unaffected by the budget.
    @InternedDistinct Object shallow(@InternedDistinct Object x) {
        return id(x);
    }
}
