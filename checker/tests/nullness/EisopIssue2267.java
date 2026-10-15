// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2267
// The constructor of an anonymous subclass of an inner class takes the enclosing instance as its
// first parameter if the class instance creation expression is qualified.  Argument checking
// of a varargs constructor matched the arguments against the wrong parameters and crashed.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2267 {
    class Inner<T> {
        Inner(String... args) {}
    }

    class NonNullInner {
        NonNullInner(String... args) {}
    }

    void m(EisopIssue2267 n, @Nullable String nbl) {
        n.new Inner<String>("a", "b") {};
        n.new Inner<String>() {};
        n.new Inner<String>("a") {};
        // :: error: (argument.type.incompatible)
        n.new Inner<String>("a", nbl) {};
        // :: error: (argument.type.incompatible)
        n.new NonNullInner(nbl) {};
    }
}
