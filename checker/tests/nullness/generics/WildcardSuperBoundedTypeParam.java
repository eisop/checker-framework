import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Regression/documentation test for the JDK-8054309 super-wildcard-collapse check ({@code
 * type.invalid.super.wildcard} in {@code BaseTypeValidator}), for the case where the wildcard's
 * extends bound comes not from an annotation written on the wildcard token itself (as in {@code
 * WildcardAnnos.java}), but from the declared bound of the type parameter, propagated during
 * capture conversion. This mirrors the shape of the jspecify conformance sample {@code
 * SuperObjectUnspec} ({@code Lib<T extends @Nullable Object>}, {@code Lib<?
 * super @NullnessUnspecified Object>}), reduced to the standard Nullness Checker's two-valued
 * lattice (no {@code @NullnessUnspecified}-style middle qualifier).
 */
public class WildcardSuperBoundedTypeParam {
    interface Lib<T extends @Nullable Object> {
        void useT(T t);
    }

    // The super bound target (Object) is the same as the erasure of Lib's type parameter bound,
    // so javac's capture conversion collapses this wildcard (JDK-8054309): no fresh captured type
    // variable is created, and the Checker Framework uses only the super bound's annotation
    // (@NonNull), ignoring the propagated extends bound (@Nullable, from T's declared bound).
    // Since the two disagree, this must be reported.
    // :: error: (type.invalid.super.wildcard)
    void differing(Lib<? super @NonNull Object> lib) {}

    // Here the explicit super bound annotation (@Nullable) agrees with the propagated extends
    // bound (@Nullable, from T's declared bound), so no error is expected.
    void agreeing(Lib<? super @Nullable Object> lib) {}
}
