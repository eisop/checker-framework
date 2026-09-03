import org.checkerframework.checker.nullness.qual.Nullable;
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
        void method(@Nullable Object o) {
            o.toString();
        }
    }

    @AnnotatedFor("nullness")
    class Lambdas {
        // A lambda body is in the scope of the @UnannotatedFor method that contains it, even
        // though a lambda is not itself a declaration.
        @UnannotatedFor("nullness")
        Runnable excluded(@Nullable Object o) {
            return () -> o.toString();
        }

        Runnable included(@Nullable Object o) {
            // :: error: (dereference.of.nullable)
            return () -> o.toString();
        }
    }

    @AnnotatedFor("nullness")
    class C {
        // @UnannotatedFor only subtracts from the enclosing scope, so a nested @AnnotatedFor takes
        // effect again.
        @UnannotatedFor("nullness")
        class Excluded {
            Object unannotated = null;

            @AnnotatedFor("nullness")
            void reannotated(@Nullable Object o) {
                // :: error: (dereference.of.nullable)
                o.toString();
            }
        }

        // An @UnannotatedFor for a different checker does not exclude this class.
        @UnannotatedFor("regex")
        class UnannotatedForOtherChecker {
            // :: error: (assignment.type.incompatible)
            Object o = null;
        }
    }
}
