/*
 * @test
 *
 * @summary Test permissive defaults with other unchecked-source options and combinations.
 * @compile/fail/ref=AnnotatedForWithUsePermissiveDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source -AuseConservativeDefaultsForUncheckedCode=bytecode AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseConservativeDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source -AusePermissiveDefaultsForUncheckedCode=bytecode AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUsePermissiveDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source,bytecode AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUsePermissiveDefaultOnlyAnnotatedFor.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source -AonlyAnnotatedFor AnnotatedForWithUse.java
 * @compile/fail/ref=ConflictingDefaultModesSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source -AuseConservativeDefaultsForUncheckedCode=source PermissiveDefaultOptions.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source UnannotatedForWithUse.java
 */

public class PermissiveDefaultOptions {}
