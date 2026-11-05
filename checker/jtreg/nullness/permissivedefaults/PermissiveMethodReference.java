/*
 * @test
 *
 * @summary Test that a permissive default in the signature of a functional interface of unchecked code does not constrain a method reference, unlike a written qualifier, and that a referenced method of unchecked code is checked as at a call.
 *
 * @compile -proc:none -parameters ../permissivedefaultslib/MethodRefLib.java
 * @compile/fail/ref=PermissiveMethodReference.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=bytecode PermissiveMethodReference.java
 * @compile/fail/ref=PermissiveMethodReferenceNoOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveMethodReference.java
 * @compile/fail/ref=PermissiveMethodReference.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source ../permissivedefaultslib/MethodRefLib.java PermissiveMethodReference.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import permissivedefaultslib.MethodRefLib;
import permissivedefaultslib.MethodRefLib.Callback;
import permissivedefaultslib.MethodRefLib.Item;
import permissivedefaultslib.MethodRefLib.ItemHandler;
import permissivedefaultslib.MethodRefLib.Namer;
import permissivedefaultslib.MethodRefLib.NullableCallback;

@AnnotatedFor("nullness")
public class PermissiveMethodReference {
    static void nonNullParameter(String value) {}

    static @Nullable String nullableReturn() {
        return null;
    }

    // A referenced method of unchecked code is checked as at a call: its permissive defaults apply,
    // and its unannotated type argument and vararg component get the ordinary default.
    Consumer<@Nullable String> unannotated = MethodRefLib::unannotated;
    Consumer<@Nullable String> nonNull = MethodRefLib::nonNull;
    Consumer<List<@Nullable String>> unannotatedElement = MethodRefLib::unannotatedElement;
    Supplier<@NonNull Object> unannotatedReturn = MethodRefLib::unannotatedReturn;
    Consumer<@Nullable String> varargs = MethodRefLib::varargs;

    // A functional interface of unchecked code.
    Callback callback = PermissiveMethodReference::nonNullParameter;
    Namer namer = PermissiveMethodReference::nullableReturn;
    NullableCallback nullableCallback = PermissiveMethodReference::nonNullParameter;
    ItemHandler unbound = Item::use;
}
