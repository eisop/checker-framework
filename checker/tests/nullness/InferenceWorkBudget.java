// Regression test for the Java 8 type-argument-inference work budget
// (Java8InferenceContext.MAX_INCORPORATION_WORK).  Incorporating bounds to a fixed point is roughly
// cubic in the nesting depth of a generic invocation, so a deeply enough nested invocation would
// otherwise take many seconds.  The budget abandons it and reports `type.argument.inference.budget`
// (a deliberate give-up, not a crash), telling the user to supply explicit type arguments.
//
// Each level does a lot of incorporation work -- g's return type mentions its type variable three
// times and f takes wildcards -- so only a shallow chain (kept to one line) is needed to exceed the
// budget, instead of the ~40-deep `id(id(...))` chain a single-type-variable method would require.
// If the budget or the per-incorporation cost changes, deepen or shorten the chain to match.
//
// The budget is a framework mechanism that applies to every type system.  This test exercises it
// with the *default* budget under the Nullness Checker (which has several subcheckers);
// checker/tests/inference-budget/InferenceWorkBudget.java exercises it under the Interning Checker
// with a small -AinferenceWorkBudget.  The worklist that does the incorporation, and whose
// dependency tracking these deep chains stress, is checked for correctness by
// checker/tests/nullness/Java8InferenceWorklistStress.java.
//
// Do not add @SuppressWarnings("nullness") to tooDeeplyNested: "nullness" is the checker name and
// would suppress the framework error under test.
public class InferenceWorkBudget {

    static <T> Triple<T, T, T> g(T x) {
        return new Triple<>();
    }

    // The body is irrelevant; only this method's signature matters for the inference under test.
    @SuppressWarnings("nullness")
    static <T> T f(Triple<T, ? extends T, ? extends T> p) {
        return null;
    }

    static class Triple<X, Y, Z> {}

    String tooDeeplyNested(String x) {
        // :: error: (type.argument.inference.budget)
        return f(g(f(g(f(g(f(g(f(g(f(g(f(g(f(g(f(g(f(g(f(g(f(g(f(g(x))))))))))))))))))))))))));
    }

    // A shallow invocation stays well under the budget and infers normally.
    Object shallow(Object x) {
        return f(g(x));
    }
}
