import org.checkerframework.framework.qual.AnnotatedFor;

public class NotAnnotatedForNoError {
    @AnnotatedFor("nullness")
    class A {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    class B {
        Object o = null;
    }
}
