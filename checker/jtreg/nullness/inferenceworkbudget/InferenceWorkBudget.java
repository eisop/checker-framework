/*
 * @test
 *
 * @summary Test that -AinferenceWorkBudget is validated once at startup, even in a file whose
 * generic method calls would otherwise read it during type-argument inference.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AinferenceWorkBudget=100 InferenceWorkBudget.java
 * @compile/fail/ref=NotANumber.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AinferenceWorkBudget=abc InferenceWorkBudget.java
 * @compile/fail/ref=Zero.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AinferenceWorkBudget=0 InferenceWorkBudget.java
 */

import java.util.Collections;
import java.util.List;

public class InferenceWorkBudget {
    List<String> empty() {
        return Collections.emptyList();
    }
}
