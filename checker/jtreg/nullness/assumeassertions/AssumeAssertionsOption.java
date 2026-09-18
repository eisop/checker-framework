/*
 * @test
 *
 * @summary Test the values of -AassumeAssertions, and that the options it replaced are diagnosed.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeAssertions=enabled AssumeAssertionsOption.java
 * @compile/fail/ref=AssumeAssertionsNeither.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeAssertions=neither AssumeAssertionsOption.java
 * @compile/fail/ref=AssumeAssertionsBadValue.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeAssertions=true AssumeAssertionsOption.java
 * @compile/fail/ref=AssumeAssertionsNoValue.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeAssertions AssumeAssertionsOption.java
 * @compile/fail/ref=RemovedEnabled.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeAssertionsAreEnabled AssumeAssertionsOption.java
 * @compile/fail/ref=RemovedDisabled.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeAssertionsAreDisabled AssumeAssertionsOption.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

public class AssumeAssertionsOption {
    int length(@Nullable String s) {
        assert s != null;
        return s.length();
    }
}
