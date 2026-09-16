import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;

public class LocalVariableRefinement {
    static class Acceptor {
        static void accept1(@Mutable Object o) {}

        static void accept2(@Immutable Object o) {}
    }

    void test1() {
        @Readonly Object rowNames = null;
        Acceptor.accept1(rowNames);
        Acceptor.accept2(rowNames);
    }

    void test2() {
        String s = null;
        Acceptor.accept1(s);
        Acceptor.accept2(s);
    }

    void test3(@Readonly Object o) {
        @Readonly Object lo = o;
        // :: error: (argument.type.incompatible)
        Acceptor.accept1(lo);
        // :: error: (argument.type.incompatible)
        Acceptor.accept2(lo);
    }

    void foo() {
        @Readonly Object o = new @Immutable Object();
        // o is refined to @Immutable
        // :: error: (argument.type.incompatible)
        Acceptor.accept1(o); // takes @Mutable
        Acceptor.accept2(o); // takes @Immutable, OK

        o = new @Mutable Object();
        // o is refined to @Mutable
        Acceptor.accept1(o); // takes @Mutable, OK
        // :: error: (argument.type.incompatible)
        Acceptor.accept2(o); // takes @Immutable
    }
}
