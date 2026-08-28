/*
 * @test
 * @summary An AnnotatedFor annotation supplied by a stub file puts source code in an
 * AnnotatedFor scope, so -AonlyAnnotatedFor no longer suppresses warnings in it.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AonlyAnnotatedFor SuppressionTest.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AonlyAnnotatedFor -AmergeStubsWithSource -Astubs=SuppressionTest.astub SuppressionTest.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source SuppressionTest.java
 * @compile/fail/ref=WithStubConservativeSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source -AmergeStubsWithSource -Astubs=SuppressionTest.astub SuppressionTest.java
 */

package annotatedforsuppression;

public class SuppressionTest {
    // Without the stub, -AonlyAnnotatedFor suppresses this assignment error.  With the stub
    // supplying AnnotatedFor("nullness") for this class, the error is reported.
    Object f = null;
}
