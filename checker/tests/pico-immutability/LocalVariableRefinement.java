import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;

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

    // TODO revisit this method
    void foo() {
        @Readonly Object o = new @Immutable Object();
        o = new @Mutable Object();
    }
}
