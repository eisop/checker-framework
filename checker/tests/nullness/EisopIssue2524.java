import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2524 {
    interface TwoNullable {
        void run(String a, @Nullable String b);
    }

    interface TwoNonNull {
        void run(String a, String b);
    }

    interface ThreeNullable {
        void run(int n, String a, @Nullable String b);
    }

    interface MakeFoo {
        Foo make(String a, @Nullable String b);
    }

    interface UnboundTwoNullable {
        void run(Foo f, String a, @Nullable String b);
    }

    static class Foo {
        Foo(String... xs) {}

        void vm(String... xs) {}
    }

    static void va(String... xs) {}

    static void vn(int n, String... xs) {}

    // :: error: (methodref.param.invalid)
    TwoNullable bad = EisopIssue2524::va;

    TwoNonNull ok = EisopIssue2524::va;

    // :: error: (methodref.param.invalid)
    ThreeNullable afterFirst = EisopIssue2524::vn;

    // :: error: (methodref.param.invalid)
    MakeFoo constructor = Foo::new;

    // :: error: (methodref.param.invalid)
    UnboundTwoNullable unbound = Foo::vm;
}
