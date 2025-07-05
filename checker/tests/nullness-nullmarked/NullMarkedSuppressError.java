public class NullMarkedSuppressError {
    @org.jspecify.annotations.NullMarked
    class A {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    class B {
        Object o = null;
    }
}
