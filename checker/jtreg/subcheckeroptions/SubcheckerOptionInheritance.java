/*
 * @test
 *
 * @summary Test that options prefixed with a parent checker name (e.g. -ANullnessChecker_...)
 * are inherited by its subcheckers (e.g. NullnessNoInitSubchecker), that a subchecker-specific
 * option overrides a parent-checker option, and that checker-specific options take precedence
 * over unprefixed options.
 *
 * @compile -proc:none ../nullness/permissivedefaultslib/Lib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeInitialized SubcheckerOptionInheritance.java
 * @compile/fail/ref=SubcheckerOptionInheritance.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeInitialized -ANullnessChecker_useConservativeDefaultsForUncheckedCode=bytecode -ANullnessChecker_lint=monotonicNonNullOnStatic SubcheckerOptionInheritance.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeInitialized -ANullnessChecker_useConservativeDefaultsForUncheckedCode=bytecode -ANullnessNoInitSubchecker_useConservativeDefaultsForUncheckedCode=-bytecode -ANullnessChecker_lint=monotonicNonNullOnStatic -ANullnessNoInitSubchecker_lint=-monotonicNonNullOnStatic SubcheckerOptionInheritance.java
 * @compile/fail/ref=SubcheckerOptionInheritance.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AassumeInitialized -AuseConservativeDefaultsForUncheckedCode=-bytecode -ANullnessChecker_useConservativeDefaultsForUncheckedCode=bytecode -Alint=-monotonicNonNullOnStatic -ANullnessChecker_lint=monotonicNonNullOnStatic SubcheckerOptionInheritance.java
 */

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import permissivedefaultslib.Lib;

public class SubcheckerOptionInheritance {

    static @MonotonicNonNull Object staticField;

    void test() {
        Lib.getObject().toString();
    }
}
