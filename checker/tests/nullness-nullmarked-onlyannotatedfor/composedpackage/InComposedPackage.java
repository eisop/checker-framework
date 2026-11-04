package composed;

// The @NullMarked alias composes with the written @AnnotatedFor("index"), so this package is
// annotated for nullness too.
public class InComposedPackage {
    // :: error: (assignment.type.incompatible)
    Object o = null;
}
