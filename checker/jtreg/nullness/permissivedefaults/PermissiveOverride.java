/*
 * @test
 *
 * @summary Test that a permissive default for a parameter of unchecked code does not constrain an override.
 * @compile -proc:none ../permissivedefaultslib/OverrideLib.java
 * @compile/fail/ref=PermissiveOverride.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode PermissiveOverride.java
 * @compile/fail/ref=PermissiveOverride.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveOverride.java
 */

import permissivedefaultslib.OverrideLib;

public class PermissiveOverride extends OverrideLib {
    // The permissive default makes the overridden parameter @Nullable, but it is not written, so
    // a non-null parameter is allowed.
    @Override
    public void unannotated(Object value) {}

    // The overridden parameter is @Nullable as written, so a non-null parameter is not allowed.
    @Override
    public void nullable(Object value) {}
}
