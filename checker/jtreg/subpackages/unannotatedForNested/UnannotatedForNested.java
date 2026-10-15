/*
 * @test
 * @summary An UnannotatedFor on a package applies to subpackages, so it excludes them from an
 * enclosing package's AnnotatedFor. The innermost package annotation wins in both directions: an
 * AnnotatedFor on a further-nested package takes effect again.
 *
 * @compile/fail/ref=UnannotatedForNested.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode pkg/package-info.java pkg/sub/package-info.java pkg/sub/deep/Deep.java pkg/sub/reann/package-info.java pkg/sub/reann/deeper/Deeper.java
 */
public class UnannotatedForNested {}
