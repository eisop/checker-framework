/*
 * @test
 * @summary Test that -AwarnUnneededSuppressions also works together with -AassumeInitialized.
 * See https://github.com/eisop/checker-framework/issues/2029
 *
 * @compile/fail/ref=UnneededSuppressionsAssumeInitialized.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions UnneededSuppressionsAssumeInitialized.java
 * @compile/fail/ref=UnneededSuppressionsAssumeInitialized.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AwarnUnneededSuppressions -AassumeInitialized UnneededSuppressionsAssumeInitialized.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

class UnneededSuppressionsAssumeInitialized {

    int needed(@Nullable String s) {
        @SuppressWarnings("nullness") // needed: suppresses dereference.of.nullable
        int n = s.length();
        return n;
    }

    @SuppressWarnings("nullness") // unneeded: there is nothing to suppress
    int useless() {
        return 1;
    }

    int reported(@Nullable String s) {
        return s.length();
    }
}
