/*
 * @test
 * @summary Test that the search for an applicable AnnotatedFor annotation walks the enclosing
 *          elements of a bytecode element: method, nested class, outer class, and package.
 *          The library is compiled by plain javac, without the Checker Framework, which is how
 *          a partially-annotated library is normally built.
 *
 * @compile ../annotatedForScopeLib/package-info.java ../annotatedForScopeLib/InPkg.java ../annotatedForScopeLib/Nesting.java ../annotatedForScopeLib/AnnotatedOuter.java ../annotatedForScopeLib/MethodScope.java ../annotatedForScopeLib/OtherChecker.java
 * @compile/fail/ref=UseScopes.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=bytecode UseScopes.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;

import annotatedforscopelib.AnnotatedOuter;
import annotatedforscopelib.MethodScope;
import annotatedforscopelib.Nesting;
import annotatedforscopelib.OtherChecker;
import annotatedforscopelib.annotatedpkg.InPkg;

// The library was not compiled with the Checker Framework, so none of these return types carries
// a defaulted annotation in the bytecode; only the AnnotatedFor annotations written in its source
// survive there.  Whether a use gets the source-code default (@NonNull) or the conservative
// bytecode default (@Nullable) therefore depends only on whether an applicable AnnotatedFor
// annotation is found on the method or on one of its enclosing elements.
public class UseScopes {
    void use() {
        // AnnotatedFor on the nested class itself.
        @NonNull Object a = Nesting.AnnotatedNested.get();

        // Nothing in scope: neither PlainNested nor the enclosing Nesting class is annotated.
        @NonNull Object b = Nesting.PlainNested.get();

        // AnnotatedFor on the enclosing class.
        @NonNull Object c = AnnotatedOuter.Nested.get();

        // AnnotatedFor on the method itself.
        @NonNull Object d = MethodScope.annotated();

        // A sibling method's AnnotatedFor does not apply.
        @NonNull Object e = MethodScope.plain();

        // AnnotatedFor names a different checker.
        @NonNull Object f = OtherChecker.get();

        // AnnotatedFor on the enclosing package, read from package-info.class.
        @NonNull Object g = InPkg.get();
    }
}
