// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2106
// The constructor of an anonymous subclass of an inner class takes the enclosing instance as its
// first parameter if the class instance creation expression is qualified.  Inference of a diamond
// matched the arguments against the wrong parameters and crashed.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2106Anonymous<Y> {
    Y y;

    EisopIssue2106Anonymous(Y y) {
        this.y = y;
    }

    class Inner<T> {
        T t;

        Inner(T t) {
            this.t = t;
        }

        Y outer() {
            return y;
        }
    }

    static void test(EisopIssue2106Anonymous<@Nullable String> n) {
        n.new Inner<>("a") {}.t.length();
        // :: error: (dereference.of.nullable)
        n.new Inner<>("a") {}.outer().length();
    }

    void implicitThis() {
        new Inner<>("a") {}.t.length();
    }
}
