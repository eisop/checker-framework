/*
 * @test
 * @summary An UnannotatedFor with applyToSubpackages=false limits only its own annotation. An
 * enclosing package whose AnnotatedFor applies to subpackages still reaches through it, so code in
 * the nested subpackage is checked rather than given conservative defaults.
 *
 * @compile/fail/ref=UnannotatedForOptOutNested.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode uafoptout/package-info.java uafoptout/sub/package-info.java uafoptout/sub/InSubpackage.java uafoptout/sub/deep/Deep.java
 */
public class UnannotatedForOptOutNested {}
