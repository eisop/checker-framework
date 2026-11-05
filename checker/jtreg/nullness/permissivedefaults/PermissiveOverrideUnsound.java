/*
 * @test
 *
 * @summary Test the unsoundness of permissive defaults for overrides: a call through an unchecked supertype is checked against its permissive defaults, not against the checked overrider.
 *
 * @compile -proc:none ../permissivedefaultslib/UnsoundLib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode PermissiveOverrideUnsound.java
 * @compile/fail/ref=PermissiveOverrideUnsoundNoOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveOverrideUnsound.java
 * @compile/fail/ref=PermissiveOverrideUnsoundConservative.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode PermissiveOverrideUnsound.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import permissivedefaultslib.UnsoundLib;

// This test records a known unsoundness, not a requirement. With permissive defaults, the
// unannotated parameter of UnsoundLib.take is @Nullable and the return type of UnsoundLib.get is
// @NonNull. Neither constrains an override, so Impl may require a non-null argument and return
// null. A call through UnsoundLib is checked against UnsoundLib's defaults, so the calls in Client
// are accepted, and both throw a NullPointerException in checked code. Without the option, and
// with conservative defaults, the checker reports an error.
@AnnotatedFor("nullness")
public class PermissiveOverrideUnsound {
    static class Impl extends UnsoundLib {
        @Override
        public void take(String value) {
            value.length();
        }

        @Override
        public @Nullable String get() {
            return null;
        }
    }

    static class Client {
        void use() {
            UnsoundLib lib = new Impl();
            lib.take(null);
            lib.get().length();
        }
    }
}
