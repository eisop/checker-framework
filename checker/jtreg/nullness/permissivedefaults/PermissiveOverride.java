/*
 * @test
 *
 * @summary Test that a permissive default for a parameter of unchecked bytecode does not constrain an override, unlike a written qualifier.
 * @compile -proc:none ../permissivedefaultslib/OverrideLib.java
 * @compile/fail/ref=PermissiveOverride.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode PermissiveOverride.java
 * @compile/fail/ref=PermissiveOverrideNoOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveOverride.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

import permissivedefaultslib.OverrideLib;

public class PermissiveOverride extends OverrideLib {
    // Only the permissive default makes the overridden parameter @Nullable, so a non-null
    // parameter is allowed.
    @Override
    public void unannotated(Object value) {}

    // The overridden parameter is @Nullable as written, so a non-null parameter is not allowed.
    @Override
    public void nullable(Object value) {}

    // The overridden List has no written qualifier, but its element type is written @Nullable.
    @Override
    public void nullableElement(List<@Nullable Object> values) {}

    @Override
    public void nullableElementNarrowed(List<Object> values) {}

    // The overridden array has no written qualifier, but its component type is written @Nullable.
    @Override
    public void nullableComponent(@Nullable Object[] values) {}

    @Override
    public void nullableComponentNarrowed(Object[] values) {}

    // No qualifier is written on the overridden List or its element type.
    @Override
    public void unannotatedElement(List<String> values) {}

    // No qualifier is written on the bound of the overridden wildcard.
    @Override
    public void unannotatedBound(List<? extends Object> values) {}

    // The bound of the overridden wildcard is written @Nullable.
    @Override
    public void nullableBoundNarrowed(List<? extends Object> values) {}
}
