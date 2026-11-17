import java.util.Arrays;
import java.util.List;

// Regression test for resolving inference variables that depend on no other unresolved variable
// together.  Each diamond argument adds inference variables that depend only on the argument's own
// arguments; resolving them one at a time saved and restored the bounds of every variable, and
// re-incorporated the instantiations into every bound, once per variable, so the work grew
// quadratically in the number of arguments and this call exceeded the inference work budget of
// this directory (-AinferenceWorkBudget=2000; see InferenceBudgetTest).
public class InferenceIndependentVariables {

    static class Pair<A, B> {
        Pair(A a, B b) {}
    }

    List<?> manyDiamondArguments() {
        return Arrays.asList(
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1),
                new Pair<>("a", 1));
    }
}
