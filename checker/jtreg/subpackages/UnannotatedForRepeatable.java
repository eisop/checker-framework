/*
 * @test
 *
 * @summary Writing two UnannotatedFor annotations at one location is what needs UnannotatedFor to
 * be Repeatable: it lets two checkers have different applyToSubpackages settings on the same
 * package. javac exposes this as a single UnannotatedFor.List container instead of two bare
 * UnannotatedFor mirrors, so resolving it needs its own List-unpacking step, mirroring
 * AnnotatedFor/AnnotatedFor.List.
 *
 * Each compile line runs one checker at a time, for the reason given in AnnotatedForRepeatable.
 * The nullness line expects a clean compile: if the List were not unpacked, both UnannotatedFor
 * entries would be invisible and package urepeatable's AnnotatedFor would reach both files.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode urepeatable/package-info.java urepeatable/sub/package-info.java urepeatable/sub/InUnannotatedSub.java urepeatable/sub/deep/Deep.java
 * @compile/fail/ref=UnannotatedForRepeatableIndex.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode urepeatable/package-info.java urepeatable/sub/package-info.java urepeatable/sub/InUnannotatedSub.java urepeatable/sub/deep/Deep.java
 */
public class UnannotatedForRepeatable {}
