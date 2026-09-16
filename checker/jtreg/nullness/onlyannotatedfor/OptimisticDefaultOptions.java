/*
 * @test
 *
 * @summary Test optimistic defaults with other unchecked-source options.
 * @compile/fail/ref=AnnotatedForWithUseOptimisticDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source -AuseConservativeDefaultsForUncheckedCode=bytecode AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseOptimisticDefaultOnlyAnnotatedFor.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source -AonlyAnnotatedFor AnnotatedForWithUse.java
 */

public class OptimisticDefaultOptions {}
