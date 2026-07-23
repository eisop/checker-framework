import org.checkerframework.framework.testchecker.util.Odd;

// DISABLED regression test: run by DisabledIntersectionBoundAnnosTest, which is
// @Ignore'd. The fix it verifies changes intersection-bound defaulting/subtyping
// semantics that the standard Nullness Checker tests deliberately rely on, so it
// is out of scope for a contained bug fix. See cf-tasks2/task-2-findings.md.
//
// Regression test for per-bound annotation preservation on intersection-type
// upper bounds. The unannotated bound must retain its default (top) qualifier
// instead of being homogenized to the other bound's explicit @Odd. Both bounds
// are interfaces so the two declarations can differ only in bound order.
// See AnnotatedIntersectionType.copyIntersectionBoundAnnotations.
public class IntersectionBoundAnnos {

    interface IfaceA {}

    interface IfaceB {}

    <T extends @Odd IfaceA & IfaceB> void oddFirst(T t) {
        // The IfaceA bound is @Odd; t is assignable to @Odd IfaceA.
        @Odd IfaceA ok = t;
        // IfaceB is unannotated, so t viewed as IfaceB is top, not @Odd. Before
        // the fix this spuriously type-checked because the IfaceB bound was
        // homogenized to the other bound's @Odd.
        // :: error: (assignment.type.incompatible)
        @Odd IfaceB bad = t;
    }

    // Order-dependence guard: the @Odd bound comes second. The unannotated IfaceB
    // bound must still default to top, independent of the bound order and of the
    // order in which methods (and thus type parameters) are processed.
    <T extends IfaceB & @Odd IfaceA> void oddSecond(T t) {
        @Odd IfaceA ok = t;
        // :: error: (assignment.type.incompatible)
        @Odd IfaceB bad = t;
    }
}
