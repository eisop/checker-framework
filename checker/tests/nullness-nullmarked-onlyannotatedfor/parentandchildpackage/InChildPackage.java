package parent.child;

// JSpecify specifies that @NullMarked on a package does not cover its subpackages, so the
// @AnnotatedFor("nullness") that package parent aliases to does not reach here.  Under
// -AonlyAnnotatedFor that means no error is reported, matching the @DefaultQualifier half of
// the alias, which likewise stops at package parent.
public class InChildPackage {
    // No expected error, because this subpackage is not annotated for nullness.
    Object o = null;
}
