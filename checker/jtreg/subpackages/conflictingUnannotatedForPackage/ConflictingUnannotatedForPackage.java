/*
 * @test
 * @summary The other order of the contradiction in conflictingAnnotatedForPackage: the
 * UnannotatedFor is written first, so it wins and the package is not checked. The warning is still
 * issued. This is what distinguishes resolving the pair by source order from letting the
 * AnnotatedFor always win, under which this package would be checked.
 *
 * @compile/ref=ConflictingUnannotatedForPackage.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source pkg/package-info.java pkg/InPkg.java
 */
public class ConflictingUnannotatedForPackage {}
