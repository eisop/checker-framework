/*
 * @test
 * @summary eisop#2037: a DefaultQualifier is lost for deeper subpackages when an intervening
 * package shadows it (same location, same qualifier hierarchy) and sets applyToSubpackages =
 * false. Package dq sets a FIELD default of Nullable; package dq.sub shadows it with a FIELD
 * default of NonNull that does not apply to subpackages. Package dq.sub's own elements correctly
 * see NonNull (shadowing is not in question), but package dq.sub.deep must still see dq's
 * Nullable default -- nothing there shadows it, and dq.sub's non-propagating shadow must not
 * remove it for deeper packages, only for dq.sub itself. Conversely, package dq.prop shadows dq
 * with a FIELD default of NonNull that does apply to subpackages (applyToSubpackages = true), and
 * dq.prop.deep must see dq.prop's NonNull default.
 *
 * @compile/fail/ref=DefaultQualifierNested.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker dq/package-info.java dq/sub/package-info.java dq/prop/package-info.java dq/InDq.java dq/sub/InDqSub.java dq/sub/deep/Deep.java dq/prop/deep/DeepProp.java
 */
public class DefaultQualifierNested {}
