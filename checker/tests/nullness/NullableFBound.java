// A type variable in its own bound has the declared bound: in <T extends @Nullable Comparable<T>>,
// the T in Comparable<T> may be null, but the annotated JDK declares
// Comparable<T extends @NonNull Object>. A method's F-bound was accepted although the same bound
// on a class, or through a second type parameter, was rejected.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NullableFBound {

    // :: error: (type.argument.type.incompatible)
    <T extends @Nullable Comparable<T>> void method(T p) {}

    // :: error: (type.argument.type.incompatible)
    static class Cls<T extends @Nullable Comparable<T>> {}

    // :: error: (type.argument.type.incompatible)
    <S extends @Nullable Object, T extends Comparable<S>> void viaOther() {}

    <T extends @NonNull Comparable<T>> void nonNull(T p) {}

    <T extends Comparable<T>> void unannotated(T p) {}

    interface Cmp<T extends @Nullable Object> {
        T self();
    }

    // The F-bound is fine where the type parameter allows null, and its uses keep the bound.
    <T extends @Nullable Cmp<T>> void nullableCmp(@NonNull T p) {
        // :: error: (dereference.of.nullable)
        p.self().toString();
    }

    <T extends @NonNull Cmp<T>> void nonNullCmp(T p) {
        p.self().toString();
    }
}
