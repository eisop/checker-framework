import org.checkerframework.framework.qual.AnnotatedFor;
import org.jspecify.annotations.NullMarked;

// A written @AnnotatedFor -- whether a single instance or, since @AnnotatedFor is @Repeatable,
// two or more written at the same location -- composes with the @NullMarked alias rather than
// being hidden by it.
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

    // Two written @AnnotatedFor collapse into a single @AnnotatedFor.List, which has a
    // different annotation name than @AnnotatedFor itself; the @NullMarked alias must still be
    // found alongside it.
    @AnnotatedFor("index")
    @AnnotatedFor("regex")
    @NullMarked
    class TwoWrittenAndNullMarked {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    // Same as above, without @NullMarked: neither written @AnnotatedFor names nullness, so
    // nullness stays suppressed.
    @AnnotatedFor("index")
    @AnnotatedFor("regex")
    class TwoWrittenNeitherNullness {
        // No expected error: nothing here is annotated for nullness.
        Object o = null;
    }
}
