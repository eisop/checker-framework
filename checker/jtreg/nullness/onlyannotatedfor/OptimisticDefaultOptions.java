/*
 * @test
 *
 * @summary Test optimistic defaults with other unchecked-source options and combinations.
 * @compile/fail/ref=AnnotatedForWithUseOptimisticDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source -AuseConservativeDefaultsForUncheckedCode=bytecode AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseConservativeDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source -AuseOptimisticDefaultsForUncheckedCode=bytecode AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseOptimisticDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source,bytecode AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseOptimisticDefaultOnlyAnnotatedFor.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source -AonlyAnnotatedFor AnnotatedForWithUse.java
 * @compile/fail/ref=ConflictingDefaultModesSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source -AuseConservativeDefaultsForUncheckedCode=source OptimisticDefaultOptions.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source UnannotatedForWithUse.java
 */

public class OptimisticDefaultOptions {}
