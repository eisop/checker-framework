import org.jspecify.annotations.NullMarked;

// The default package is not @NullMarked, so under -AonlyAnnotatedFor only the @NullMarked
// nested class is checked.
public class NullMarkedSuppressError {
    @NullMarked
    class A {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    class B {
        // No expected error, because code is not annotated for nullness.
        Object o = null;
    }
}
