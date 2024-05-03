import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;

class Acceptor {
    static void accept1(@Mutable Object o) {}

    static void accept2(@Immutable Object o) {}
}

public class RefineFromNull {
    void test1() {
        // Should we allow propagation of @Bottom towards declared type?
        // We should, otherwise, refined type is always top(lub with top is top)
        @Readonly Object rowNames = null;
        // TODO Should we give warning here because of refined @Bottom? It will warn if we enforce
        // forbidding @Bottom in Validator => We don't warn @Bottom anymore, it's internal qualifier
        // now, and internal usage is always valid
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
}
