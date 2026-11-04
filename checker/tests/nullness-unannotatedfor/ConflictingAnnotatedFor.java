import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.UnannotatedFor;

// Writing both annotations for the same checker on one declaration contradicts itself: the
// @AnnotatedFor wins, so the @UnannotatedFor has no effect. Warn rather than silently pick one.
public class ConflictingAnnotatedFor {

    @AnnotatedFor("nullness")
    @UnannotatedFor("nullness")
    // :: warning: (conflicting.annotatedfor)
    static class BothOnAClass {
        // The @AnnotatedFor won, so this code is checked.
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    @AnnotatedFor("nullness")
    static class Methods {
        @AnnotatedFor("nullness")
        @UnannotatedFor("nullness")
        // :: warning: (conflicting.annotatedfor)
        void bothOnAMethod(Object o) {}

        @AnnotatedFor("nullness")
        @UnannotatedFor("nullness")
        // :: warning: (conflicting.annotatedfor)
        Methods() {}

        // Naming different checkers is not a conflict: no warning.
        @AnnotatedFor("nullness")
        @UnannotatedFor("regex")
        void differentCheckers(Object o) {}

        // Only @UnannotatedFor: no warning, and it excludes this method.
        @UnannotatedFor("nullness")
        void onlyUnannotated(Object o) {}
    }
}
