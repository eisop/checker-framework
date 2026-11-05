/*
 * @test
 *
 * @summary Test that the Value Checker replaces the IntRangeFromX qualifiers of an overridden method of unchecked code that gets permissive defaults.
 *
 * @compile -proc:none ../permissivedefaultslib/ValueLib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.common.value.ValueChecker -AusePermissiveDefaultsForUncheckedCode=bytecode ValuePermissiveOverride.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.common.value.ValueChecker ValuePermissiveOverride.java
 */

import permissivedefaultslib.ValueLib;

// @Positive is an alias of @IntRangeFromPositive, which the Value Checker replaces in the
// overridden return type with @UnknownVal, so an unannotated return type may override it.
public class ValuePermissiveOverride extends ValueLib {
    @Override
    public int positive() {
        return 0;
    }
}
