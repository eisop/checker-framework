/*
 * @test
 *
 * @summary Test that -Amode=jspecify applies permissive defaults to unannotated bytecode, that
 * turning them off restores the ordinary defaults, that conservative defaults written on the
 * command line (whether unprefixed or prefixed with the checker name) replace them instead of
 * conflicting with them, and that options prefixed for an unrelated checker do not affect defaults.
 *
 * @compile -proc:none ../permissivedefaultslib/Lib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify ModeEnablesPermissiveDefaults.java
 * @compile/fail/ref=ModeEnablesPermissiveDefaultsOff.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -AusePermissiveDefaultsForUncheckedCode=-source,-bytecode ModeEnablesPermissiveDefaults.java
 * @compile/fail/ref=ModeEnablesPermissiveDefaultsConservative.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -AuseConservativeDefaultsForUncheckedCode=bytecode ModeEnablesPermissiveDefaults.java
 * @compile/fail/ref=ModeEnablesPermissiveDefaultsConservative.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -ANullnessChecker_useConservativeDefaultsForUncheckedCode=bytecode ModeEnablesPermissiveDefaults.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify -ABogusUnrelatedChecker_useConservativeDefaultsForUncheckedCode=bytecode ModeEnablesPermissiveDefaults.java
 */

import org.checkerframework.framework.qual.AnnotatedFor;
import permissivedefaultslib.Lib;

@AnnotatedFor("nullness")
public class ModeEnablesPermissiveDefaults {
  void calls() {
    // 1: OK, 2: Err, 3: Err, 4: Err, 5: OK
    Lib.setObject(null);
    // 1: OK, 2: OK, 3: Err, 4: Err, 5: OK
    Lib.getObject().toString();
  }
}
