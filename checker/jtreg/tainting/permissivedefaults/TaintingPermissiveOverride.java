/*
 * @test
 *
 * @summary Test that a permissive default in the signature of a method of unchecked bytecode does not constrain an override in the Tainting Checker, unlike a written qualifier.
 * @compile -proc:none ../permissivedefaultslib/TaintingLib.java
 * @compile/fail/ref=TaintingPermissiveOverride.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AusePermissiveDefaultsForUncheckedCode=bytecode TaintingPermissiveOverride.java
 * @compile/fail/ref=TaintingPermissiveOverrideNoOption.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker TaintingPermissiveOverride.java
 */

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.List;

import permissivedefaultslib.TaintingLib;

// In the Tainting Checker, the top qualifier @Tainted is the default; a permissive default makes a
// parameter @Tainted and a return type @Untainted.
public class TaintingPermissiveOverride extends TaintingLib {
    // Only the permissive default makes the overridden parameter @Tainted.
    @Override
    public void unannotatedParameter(@Untainted String value) {}

    // The overridden parameter is @Tainted as written.
    @Override
    public void taintedParameter(@Untainted String value) {}

    // The overridden List has no written qualifier, but its element type is written @Tainted.
    @Override
    public void taintedElement(@Untainted List<@Tainted String> values) {}

    @Override
    public void taintedElementNarrowed(List<@Untainted String> values) {}

    // Only the permissive default makes the overridden return type @Untainted.
    @Override
    public @Tainted String unannotatedReturn() {
        return "";
    }

    // The overridden return type is @Untainted as written.
    @Override
    public @Tainted String untaintedReturn() {
        return "";
    }

    // No qualifier is written on the bounds of the overridden type parameter.
    @Override
    public <T extends @Untainted Object> void implicitBound(T value) {}
}
