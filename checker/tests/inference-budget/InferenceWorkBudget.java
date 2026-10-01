import org.checkerframework.checker.interning.qual.InternedDistinct;

// Regression test for the configurable Java 8 type-argument-inference work budget
// (Java8InferenceContext.MAX_INCORPORATION_WORK, overridable with -AinferenceWorkBudget).  This
// directory runs with a small budget (-AinferenceWorkBudget=2000; see InferenceBudgetTest), so a
// merely shallow nested generic invocation already exceeds it and is abandoned with a
// type.argument.inference.budget error -- without the 100+-deep chain a default-budget test needs.
// Lowering the budget rather than deepening the chain keeps this test compact and robust: it does
// not silently stop firing if the per-incorporation cost changes.
//
// This exercises the budget under the Interning Checker with a small -AinferenceWorkBudget;
// checker/tests/nullness/InferenceWorkBudget.java exercises it with the default budget under the
// Nullness Checker (which has several subcheckers).  The worklist whose incorporation these chains
// drive is checked for correctness by checker/tests/nullness/Java8InferenceWorklistStress.java.
public class InferenceWorkBudget {

    static <T> T id(T x) {
        return x;
    }

    String tooComplex(String x) {
        // :: error: (type.argument.inference.budget)
        return id(id(id(id(id(id(id(id(id(id(id(id(id(id(id(x)))))))))))))));
    }

    // A shallow invocation stays well under the budget and infers normally.
    @InternedDistinct Object shallow(@InternedDistinct Object x) {
        return id(x);
    }
}
