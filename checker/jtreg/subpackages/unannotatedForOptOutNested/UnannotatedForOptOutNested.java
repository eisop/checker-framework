/*
 * @test
 * @summary An UnannotatedFor with applyToSubpackages=false limits only its own annotation. An
 * enclosing package whose AnnotatedFor applies to subpackages still reaches through it, so code in
 * the nested subpackage is checked rather than given conservative defaults.
 *
 * @compile/fail/ref=UnannotatedForOptOutNested.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode pkg/package-info.java pkg/sub/package-info.java pkg/sub/InSub.java pkg/sub/deep/Deep.java
 */
public class UnannotatedForOptOutNested {}
