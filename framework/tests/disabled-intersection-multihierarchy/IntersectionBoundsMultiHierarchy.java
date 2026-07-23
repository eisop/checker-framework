import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// DISABLED regression test: run by DisabledIntersectionMultiHierarchyTest,
// which is @Ignore'd. It encodes per-element intersection-bound semantics
// (each bound keeps its own qualifiers) that are not implemented: an
// intersection type currently homogenizes all bounds to its primary
// annotations. See cf-tasks2/task-2-findings.md.
//
// With two independent hierarchies (H1 and H2), homogenization is visible even
// without conflicting annotations: for <T extends @H1S1 IfaceA & @H2S1 IfaceB>
// the primary collects @H1S1 (from the IfaceA bound) and @H2S1 (from the
// IfaceB bound) and copies both onto both bounds, so the IfaceA bound appears
// to be @H2S1 although its H2 qualifier should be the default (@H2Top), and
// symmetrically for IfaceB's H1 qualifier.
public class IntersectionBoundsMultiHierarchy {

    interface IfaceA {}

    interface IfaceB {}

    <T extends @H1S1 IfaceA & @H2S1 IfaceB> void test(T t) {
        // The IfaceA bound is @H1S1 in H1 and defaults to @H2Top in H2.
        @H1S1 IfaceA ok1 = t;
        // Under per-element semantics, t viewed as IfaceA is not @H2S1; the
        // @H2S1 written on the IfaceB bound must not leak onto IfaceA.
        // :: error: (assignment.type.incompatible)
        @H1S1 @H2S1 IfaceA bad1 = t;

        // The IfaceB bound is @H2S1 in H2 and defaults to @H1Top in H1.
        @H2S1 IfaceB ok2 = t;
        // Symmetrically, the @H1S1 written on the IfaceA bound must not leak
        // onto IfaceB.
        // :: error: (assignment.type.incompatible)
        @H1S1 @H2S1 IfaceB bad2 = t;
    }
}
