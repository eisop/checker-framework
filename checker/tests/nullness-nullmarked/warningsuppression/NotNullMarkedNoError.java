import org.jspecify.annotations.NullMarked;

public class NotNullMarkedNoError {
    @NullMarked
    class A {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    class B {
        Object o = null;
    }
}
