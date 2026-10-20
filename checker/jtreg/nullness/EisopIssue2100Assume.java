/*
 * @test
 * @summary Test that the result of a repeated call is refined only if the method is assumed to be
 * both deterministic and side-effect-free: -AassumeDeterministic or -AassumeSideEffectFree alone
 * does not refine it, and both together do. See eisop issue 2100.
 *
 * @compile/fail/ref=EisopIssue2100Assume.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker EisopIssue2100Assume.java -AassumeDeterministic
 * @compile/fail/ref=EisopIssue2100Assume.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker EisopIssue2100Assume.java -AassumeSideEffectFree
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker EisopIssue2100Assume.java -AassumeSideEffectFree -AassumeDeterministic
 */

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2100Assume {
    @Nullable Object f = new Object();

    @Nullable Object get() {
        return this.f;
    }

    void test() {
        if (get() != null) {
            get().hashCode();
        }
    }
}
