import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Test for super-bounded wildcards that javac's capture conversion collapses to their bound
 * (JDK-8054309), for the case where the wildcard's extends bound comes not from an annotation
 * written on the wildcard token itself (as in {@code WildcardAnnos.java}), but from the declared
 * bound of the type parameter, propagated during capture conversion. This mirrors the shape of the
 * jspecify conformance sample {@code SuperObjectUnspec} ({@code Lib<T extends @Nullable Object>},
 * {@code Lib<? super @NullnessUnspecified Object>}), reduced to the standard Nullness Checker's
 * two-valued lattice (no {@code @NullnessUnspecified}-style middle qualifier).
 */
public class WildcardSuperBoundedTypeParam {
    interface Lib<T extends @Nullable Object> {
        void useT(T t);

        T getT();
    }

    // The super bound target (Object) is the same as the erasure of Lib's type parameter bound,
    // so javac's capture conversion collapses this wildcard (JDK-8054309): no fresh captured type
    // variable is created. Since the super bound's annotation (@NonNull) and the propagated
    // extends bound's annotation (@Nullable, from T's declared bound) disagree, the Checker
    // Framework reconstructs the captured type variable with both bounds instead of reporting
    // the declaration; uses then see a type variable that is @Nullable when read and @NonNull
    // when written.
    void differing(Lib<? super @NonNull Object> lib) {
        // :: error: (argument.type.incompatible)
        lib.useT(null);
        lib.useT("non-null is fine");
        // :: error: (dereference.of.nullable)
        lib.getT().toString();
        // :: error: (assignment.type.incompatible)
        Lib<? extends @NonNull Object> nonNullOnly = lib;
        Lib<? extends @Nullable Object> any = lib;
    }

    // Here the explicit super bound annotation (@Nullable) agrees with the propagated extends
    // bound (@Nullable, from T's declared bound), so no error is expected.
    void agreeing(Lib<? super @Nullable Object> lib) {}

    static class Gen<T extends @Nullable Object, S extends T> {}

    // A wildcard that collapses to a type variable is not reconstructed, so the declaration is
    // still rejected.
    <F extends @Nullable Object> void collapsedTypeVariable(
            // :: error: (type.invalid.super.wildcard)
            Gen<F, ? super @NonNull F> gen) {}
}
