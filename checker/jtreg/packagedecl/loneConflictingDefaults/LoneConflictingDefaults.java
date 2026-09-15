/*
 * @test
 * @summary A conflicting DefaultQualifier pair on a package is reported even when
 * package-info.java is the only file compiled. The diagnostic used to be a side effect of
 * resolving the package's defaults, which nothing asks for when no class in the package is
 * compiled, so this was silent.
 *
 * @compile/fail/ref=LoneConflictingDefaults.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker pkg/package-info.java
 */
public class LoneConflictingDefaults {}
