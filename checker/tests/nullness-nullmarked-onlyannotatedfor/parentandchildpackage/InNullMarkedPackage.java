package parent;

// The @NullMarked on package parent aliases to @AnnotatedFor("nullness"), so errors in this
// class are reported even under -AonlyAnnotatedFor.
public class InNullMarkedPackage {
    // :: error: (assignment.type.incompatible)
    Object o = null;
}
