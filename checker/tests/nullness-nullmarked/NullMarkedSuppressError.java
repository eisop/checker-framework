public class NullMarkedSuppressError {
    @org.jspecify.annotations.NullMarked
    class A {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    class B {
        // No expected error, because code is not annotated for nullness.
        Object o = null;
    }
}
