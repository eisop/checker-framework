/*
 * @test
 *
 * @summary Test permissive defaults for bytecode, bounds, and option validation.
 * @compile -proc:none ../permissivedefaultslib/Lib.java
 * @compile/fail/ref=NoDefaults.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveDefaults.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode PermissiveDefaults.java
 * @compile/fail/ref=ConservativeDefaults.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode PermissiveDefaults.java
 * @compile/fail/ref=InvalidPermissiveOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=btyecode PermissiveDefaults.java
 * @compile/fail/ref=ConflictingPermissiveOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source,-source PermissiveDefaults.java
 * @compile/fail/ref=ConflictingDefaultModes.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode -AuseConservativeDefaultsForUncheckedCode=bytecode PermissiveDefaults.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import permissivedefaultslib.Lib;

public class PermissiveDefaults {
    void calls() {
        Lib.setObject(null);
        Lib.getObject().toString();
    }

    void bounds(Lib<@Nullable Object> nullableArgument, Lib<@NonNull Object> nonNullArgument) {
        Lib.upper(nullableArgument);
        Lib.lower(nullableArgument);
        Lib.lower(nonNullArgument);
    }
}
