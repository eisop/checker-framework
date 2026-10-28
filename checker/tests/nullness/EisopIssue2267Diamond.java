// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2267
// Like EisopIssue2267, but with a diamond, which javac 8 does not allow for anonymous classes.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2267Diamond {
    class Inner<T> {
        Inner(String... args) {}
    }

    void m(EisopIssue2267Diamond n, @Nullable String nbl) {
        n.new Inner<>("a", "b") {};
        n.new Inner<>() {};
        // :: error: (argument.type.incompatible)
        n.new Inner<>("a", nbl) {};
    }
}
