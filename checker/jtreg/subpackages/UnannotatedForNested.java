/*
 * @test
 * @summary An UnannotatedFor on a package applies to subpackages, so it excludes them from an
 * enclosing package's AnnotatedFor. The innermost package annotation wins in both directions: an
 * AnnotatedFor on a further-nested package takes effect again.
 *
 * @compile/fail/ref=UnannotatedForNested.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode uaf/package-info.java uaf/sub/package-info.java uaf/sub/deep/Deep.java uaf/sub/reann/package-info.java uaf/sub/reann/deeper/Deeper.java
 */
public class UnannotatedForNested {}
