/*
 * @test
 * @summary A stub file for a class that is being compiled is used only with
 * -AmergeStubsWithSource.  With that flag, an AnnotatedFor annotation written in the stub puts
 * the source code in an AnnotatedFor scope; without it the stub is ignored and the warning
 * stays suppressed.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AonlyAnnotatedFor SuppressionTest.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AonlyAnnotatedFor -Astubs=SuppressionTest.astub SuppressionTest.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AonlyAnnotatedFor -AmergeStubsWithSource -Astubs=SuppressionTest.astub SuppressionTest.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source SuppressionTest.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source -Astubs=SuppressionTest.astub SuppressionTest.java
 * @compile/fail/ref=WithStubConservativeSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source -AmergeStubsWithSource -Astubs=SuppressionTest.astub SuppressionTest.java
 */

package annotatedforsuppression;

public class SuppressionTest {
    Object f = null;
}
