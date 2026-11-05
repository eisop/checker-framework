/*
 * @test
 *
 * @summary Test that the defaults of unchecked code are not stored into its class file, but its written qualifiers are: a client gets its own unchecked defaults for the bytecode and the written qualifiers, however the library was compiled.
 *
 * @compile -proc:none ../permissivedefaultslib/UncheckedLib.java
 * @compile/fail/ref=UncheckedDefaultsNotStoredPermissive.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 * @compile/fail/ref=UncheckedDefaultsNotStored.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source ../permissivedefaultslib/UncheckedLib.java
 * @compile/fail/ref=UncheckedDefaultsNotStoredPermissive.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 * @compile/fail/ref=UncheckedDefaultsNotStored.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source ../permissivedefaultslib/UncheckedLib.java
 * @compile/fail/ref=UncheckedDefaultsNotStoredPermissive.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 * @compile/fail/ref=UncheckedDefaultsNotStored.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import permissivedefaultslib.UncheckedLib;

// UncheckedLib is compiled with javac, and with the Nullness Checker with conservative and with
// permissive defaults for unchecked code. Each time, this class gets the same diagnostics for the
// same option: permissive defaults accept the calls of the unannotated methods, and conservative
// defaults reject them. The Nullness qualifiers written in UncheckedLib are stored, and override
// either default.
@AnnotatedFor("nullness")
public class UncheckedDefaultsNotStored {
    void use(@Nullable Object value) {
        UncheckedLib.get().toString();
        UncheckedLib.take(value);
    }

    void useWritten(@Nullable Object value) {
        // Always an error: the written @Nullable overrides the permissive @NonNull.
        UncheckedLib.getNullable().toString();
        // Never a Nullness error: the written @NonNull overrides the conservative @Nullable.
        UncheckedLib.getNonNull().toString();
        // Never a Nullness error: the written @Nullable overrides the conservative @NonNull.
        UncheckedLib.takeNullable(value);
        // Always an error: the written @NonNull overrides the permissive @Nullable.
        UncheckedLib.takeNonNull(value);
    }
}
