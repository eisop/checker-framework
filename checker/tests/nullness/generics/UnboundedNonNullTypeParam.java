import org.checkerframework.checker.nullness.qual.*;

// Regression test for https://github.com/eisop/checker-framework/issues/1887
//
// A primary annotation on a type parameter with no explicit `extends` clause, such as `<@NonNull
// T>`, sets ONLY the type variable's lower bound.  The implicit `Object` upper bound is defaulted
// independently to the top qualifier (`@Nullable Object` for the Nullness Checker), regardless of
// the primary annotation.  This is the CLIMB-to-top rule; see the manual sections "Syntax for upper
// and lower bounds" (generics-bounds-syntax) and "Defaults" (generics-defaults), and the FAQ entry
// "Why are explicit and implicit bounds defaulted differently?".
//
// Therefore `<@NonNull T>` is NOT equivalent to `<@NonNull T extends @NonNull Object>`: the former
// still accepts a `@Nullable` type argument, while the latter rejects it.  Issue #1887 proposed
// making them equivalent, but that would break the `@KeyForBottom`-on-lower-bound override idiom
// (e.g. Collection's `<@KeyForBottom T> @Nullable T[] toArray(@PolyNull T[] a)`), which depends on
// the upper bound staying at top.  This test locks down the intended behavior.
public class UnboundedNonNullTypeParam {
    // Bare primary annotation: lower bound @NonNull, upper bound defaults to top (@Nullable
    // Object).
    static class MyList1<@NonNull T> {}

    // Explicit upper bound: the type argument must be @NonNull.
    static class MyList2<@NonNull T extends @NonNull Object> {}

    void testUnbounded() {
        // Accepted: the upper bound defaulted to @Nullable Object, so a @Nullable argument is
        // legal.
        MyList1<@Nullable String> x1 = null;
        MyList1<@NonNull String> y1 = null;
    }

    void testExplicitBound() {
        // Rejected: the explicit @NonNull upper bound forbids a @Nullable argument.
        // :: error: (type.argument.type.incompatible)
        MyList2<@Nullable String> x2 = null;
        MyList2<@NonNull String> y2 = null;
    }

    // A method type parameter behaves the same way: the implicit upper bound is top.
    static <@NonNull U> void m(U u) {}

    void testMethod() {
        m("hello");
        m((@Nullable String) null);
    }
}
