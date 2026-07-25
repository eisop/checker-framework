import org.checkerframework.checker.nullness.qual.*;

// Test for https://github.com/eisop/checker-framework/issues/1887
// A primary annotation on a type parameter with no explicit `extends` clause (`<@NonNull T>`)
// means the same as annotating the implicit `Object` upper bound (`<@NonNull T extends
// @NonNull Object>`).  Both forms must reject a `@Nullable` type argument.
public class UnboundedNonNullTypeParam {
    static class MyList1<@NonNull T> {}

    static class MyList2<@NonNull T extends @NonNull Object> {}

    void test1() {
        // :: error: (type.argument.type.incompatible)
        MyList1<@Nullable String> x1 = null;
    }

    void test2() {
        // :: error: (type.argument.type.incompatible)
        MyList2<@Nullable String> x2 = null;
    }

    // Instantiating with a non-null type argument is allowed for both forms.
    void ok(MyList1<@NonNull String> y1, MyList2<@NonNull String> y2) {}

    // A method type parameter behaves the same way.
    static <@NonNull U> void m(U u) {}

    void testMethod() {
        m("hello");
        // :: error: (type.arguments.not.inferred)
        m((@Nullable String) null);
    }
}
