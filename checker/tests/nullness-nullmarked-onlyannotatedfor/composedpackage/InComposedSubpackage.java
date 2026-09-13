package composed.sub;

// The written @AnnotatedFor("index") applies to subpackages, but it does not name nullness.
// The @NullMarked alias names nullness but sets applyToSubpackages=false.  So for the
// Nullness Checker this subpackage is not covered, and its errors stay suppressed.
public class InComposedSubpackage {
    // No expected error: @NullMarked does not reach subpackages.
    Object o = null;
}
