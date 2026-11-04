/*
 * @test
 * @summary An AnnotatedFor with applyToSubpackages=false limits only its own annotation. An
 * enclosing package whose annotation applies to subpackages still reaches through it, so code in
 * the nested subpackage is checked rather than given conservative defaults.
 *
 * @compile/fail/ref=AnnotatedForNested.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode af/package-info.java af/sub/package-info.java af/sub/deep/Deep.java
 */
public class AnnotatedForNested {}
