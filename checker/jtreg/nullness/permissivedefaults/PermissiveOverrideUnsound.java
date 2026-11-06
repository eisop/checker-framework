/*
 * @test
 *
 * @summary Test the unsoundness of permissive defaults for overrides: a call through an unchecked supertype is checked against its permissive defaults, not against the checked overrider.
 *
 * @compile -proc:none ../permissivedefaultslib/OverrideLib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode PermissiveOverrideUnsound.java
 * @compile/fail/ref=PermissiveOverrideUnsoundNoOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveOverrideUnsound.java
 * @compile/fail/ref=PermissiveOverrideUnsoundConservative.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode PermissiveOverrideUnsound.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import permissivedefaultslib.OverrideLib;

// This test records a known unsoundness, not a requirement. With permissive defaults, the
// parameter of OverrideLib.unannotated is @Nullable and the return type of
// OverrideLib.unannotatedReturn is @NonNull. Neither constrains an override, so Impl may require a
// non-null argument and return null. A call through OverrideLib is checked against OverrideLib's
// defaults, so the calls in Client are accepted, and both throw a NullPointerException in checked
// code. Without the option, and
// with conservative defaults, the checker reports an error.
@AnnotatedFor("nullness")
public class PermissiveOverrideUnsound {
    static class Impl extends OverrideLib {
        @Override
        public void unannotated(Object value) {
            value.toString();
        }

        @Override
        public @Nullable Object unannotatedReturn() {
            return null;
        }
    }

    static class Client {
        void use() {
            OverrideLib lib = new Impl();
            lib.unannotated(null);
            lib.unannotatedReturn().toString();
        }
    }
}
