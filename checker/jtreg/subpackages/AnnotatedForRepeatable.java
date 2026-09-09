/*
 * @test
 *
 * @summary A checker-qual predating this feature has no AnnotatedFor.List type at all (not just a
 * missing element on an existing type), so the classpath-compatibility check for it must look
 * before calling TreeUtils.getMethod, which throws on a wholly missing type rather than returning
 * null the way TreeUtils.getMethodOrNull does for a missing element on an existing type.
 *
 * Writing two AnnotatedFor annotations at one location -- rather than listing both checkers in
 * one, which value()'s array already supports -- is what needs AnnotatedFor to be Repeatable: it
 * lets two checkers have different applyToSubpackages settings on the same package. javac exposes
 * this as a single AnnotatedFor.List container instead of two bare AnnotatedFor mirrors, so
 * resolving it needs its own List-unpacking step, mirroring how QualifierDefaults already handles
 * DefaultQualifier and DefaultQualifier.List.
 *
 * Each compile line below runs one checker at a time against the same two-entry package-info:
 * running two top-level checkers together in one javac invocation is not used here, because doing
 * so has an unrelated, pre-existing behavior where a class already flagged by one checker's error
 * is not additionally checked by another -- confirmed with two ordinary, unrelated errors and no
 * AnnotatedFor involved at all. That is orthogonal to what this test checks.
 *
 * @compile/fail/ref=AnnotatedForRepeatableNullness.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode repeatable/package-info.java repeatable/InRepeatable.java repeatable/sub/InRepeatableSub.java
 * @compile/fail/ref=AnnotatedForRepeatableIndex.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker -AuseConservativeDefaultsForUncheckedCode=source,bytecode repeatable/package-info.java repeatable/InRepeatable.java repeatable/sub/InRepeatableSub.java
 */
public class AnnotatedForRepeatable {}
