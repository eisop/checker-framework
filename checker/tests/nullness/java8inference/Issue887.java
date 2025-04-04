// Test case for Issue 887
// https://github.com/typetools/checker-framework/issues/887
// Additional test case in framework/tests/all-systems/Issue887.java

import org.checkerframework.checker.nullness.qual.*;

import java.util.List;

public abstract class Issue887 {
    void test() {
        // :: error: (type.arguments.not.inferred)
        method(foo(null).get(0));
        methodNullable(fooNullable(null).get(0));
    }

    void method(Number o) {}

    void methodNullable(@Nullable Number o) {}

    abstract <T extends Number> List<? extends T> foo(T t);

    abstract <T extends @Nullable Number> List<? extends T> fooNullable(T t);
}
