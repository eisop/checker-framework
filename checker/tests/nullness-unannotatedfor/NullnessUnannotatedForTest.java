import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.UnannotatedFor;

public class NullnessUnannotatedForTest {
    @AnnotatedFor("nullness")
    class A {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    @AnnotatedFor("nullness")
    class B {
        @UnannotatedFor("nullness")
        void method() {
            Object o = null;
        }
    }
}
