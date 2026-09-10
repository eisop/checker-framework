/*
 * @test
 * @summary JSpecify's NullUnmarked on a package is aliased to UnannotatedFor("nullness") with
 * applyToSubpackages=false, so it excludes its own package from an enclosing AnnotatedFor but not
 * a nested subpackage.
 *
 * @compile/fail/ref=NullUnmarkedPackage.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor nup/package-info.java nup/sub/package-info.java nup/sub/InSubpackage.java nup/sub/deep/Deep.java
 */
public class NullUnmarkedPackage {}
