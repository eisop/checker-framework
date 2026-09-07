import org.checkerframework.framework.qual.AnnotatedFor;
import org.jspecify.annotations.NullMarked;

// @AnnotatedFor is not repeatable, so an element can carry at most one written one.  The
// @NullMarked alias composes with it rather than being hidden by it.
public class NullMarkedWithExplicitAnnotatedFor {

    // @AnnotatedFor("index") does not name nullness, but the @NullMarked alias does, so this
    // class is checked for nullness as well as for the Index Checker.
    @AnnotatedFor("index")
    @NullMarked
    class IndexAndNullMarked {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    // Naming nullness explicitly works, whether or not @NullMarked is also present.
    @AnnotatedFor("nullness")
    @NullMarked
    class NullnessAndNullMarked {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    // The alias applies when there is no written @AnnotatedFor.
    @NullMarked
    class OnlyNullMarked {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    // An @AnnotatedFor that names neither nullness nor an alias leaves nullness suppressed.
    @AnnotatedFor("index")
    class OnlyIndex {
        // No expected error: nothing here is annotated for nullness.
        Object o = null;
    }
}
