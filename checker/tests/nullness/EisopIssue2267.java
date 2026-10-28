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

    class Mixed<T> {
        Mixed(int i, String... args) {}
    }

    void m(EisopIssue2267 n, @Nullable String nbl) {
        n.new Inner<String>("a", "b") {};
        n.new Inner<String>() {};
        n.new Inner<String>("a") {};
        // :: error: (argument.type.incompatible)
        n.new Inner<String>("a", nbl) {};
        // :: error: (argument.type.incompatible)
        n.new NonNullInner(nbl) {};
        n.new Mixed<String>(1) {};
        n.new Mixed<String>(1, "a", "b") {};
        // :: error: (argument.type.incompatible)
        n.new Mixed<String>(1, "a", nbl) {};
        n.new Inner<String>(new String[] {"a", "b"}) {};
    }
}
