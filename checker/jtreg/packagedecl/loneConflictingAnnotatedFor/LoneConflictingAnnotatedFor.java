/*
 * @test
 * @summary A conflicting AnnotatedFor/UnannotatedFor pair on a package is reported even when
 * package-info.java is the only file compiled. Before packages were dispatched to the checker,
 * the check could only be reached through a class in the package, so this compilation was silent.
 *
 * @compile/ref=LoneConflictingAnnotatedFor.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker pkg/package-info.java
 */
public class LoneConflictingAnnotatedFor {}
