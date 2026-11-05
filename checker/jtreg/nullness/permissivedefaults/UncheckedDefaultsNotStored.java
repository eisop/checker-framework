/*
 * @test
 *
 * @summary Test that the defaults of unchecked code are not stored into its class file: a client gets its own unchecked defaults for the bytecode, however the library was compiled.
 *
 * @compile -proc:none ../permissivedefaultslib/UncheckedLib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 * @compile/fail/ref=UncheckedDefaultsNotStored.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source ../permissivedefaultslib/UncheckedLib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source ../permissivedefaultslib/UncheckedLib.java
 * @compile/fail/ref=UncheckedDefaultsNotStored.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode UncheckedDefaultsNotStored.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import permissivedefaultslib.UncheckedLib;

// UncheckedLib is compiled with javac, and with the Nullness Checker with conservative and with
// permissive defaults for unchecked code. Each time, this class gets the same diagnostics for the
// same option: permissive defaults accept the calls and the override, conservative defaults
// reject the calls.
@AnnotatedFor("nullness")
public class UncheckedDefaultsNotStored extends UncheckedLib {
    void use(@Nullable Object value) {
        UncheckedLib.get().toString();
        UncheckedLib.take(value);
    }

    @Override
    public void visit(String value) {}
}
