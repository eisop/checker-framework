/*
 * @test
 * @summary Writing both AnnotatedFor and UnannotatedFor for one checker on a package contradicts
 * itself, so the checker warns. The AnnotatedFor wins, so the package is still checked. The
 * warning is issued once, on the package declaration in package-info.java, not once per file.
 *
 * @compile/fail/ref=ConflictingAnnotatedForPackage.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source pkg/package-info.java pkg/InPkg.java
 */
public class ConflictingAnnotatedForPackage {}
