/*
 * @test
 *
 * @summary Test that a qualifier that a checked subclass gives a type variable of an unchecked superclass constrains an override, although the type variable has no written qualifier.
 *
 * @compile -proc:none ../permissivedefaultslib/GenericLib.java
 * @compile/fail/ref=PermissiveOverrideGeneric.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode PermissiveOverrideGeneric.java
 *
 * @compile/fail/ref=PermissiveOverrideGeneric.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source ../permissivedefaultslib/GenericLib.java PermissiveOverrideGeneric.java
 * @compile/fail/ref=PermissiveOverrideGeneric.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source PermissiveOverrideGeneric.java ../permissivedefaultslib/GenericLib.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import java.util.List;

import permissivedefaultslib.GenericLib;

@AnnotatedFor("nullness")
public class PermissiveOverrideGeneric {
    // The extends clause writes @Nullable for T.
    static class NullableArgument extends GenericLib<@Nullable String> {
        @Override
        public void consume(@Nullable String value) {}

        @Override
        public void consumeAll(List<@Nullable String> values) {}

        // The type variable's qualifier is unwritten in GenericLib, but the substituted one is not.
        static class Narrowed extends GenericLib<@Nullable String> {
            @Override
            public void consume(String value) {}

            @Override
            public void consumeAll(List<String> values) {}
        }
    }

    // The extends clause gives T the default @NonNull.
    static class DefaultArgument extends GenericLib<String> {
        @Override
        public String produce() {
            return "";
        }

        @Override
        public void consumeAll(List<String> values) {}

        // Only the permissive default makes this parameter @Nullable.
        @Override
        public void unannotated(@NonNull String value) {}

        static class Widened extends GenericLib<String> {
            @Override
            public @Nullable String produce() {
                return null;
            }

            @Override
            public void consumeAll(List<@Nullable String> values) {}
        }
    }
}
