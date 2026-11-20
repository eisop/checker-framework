package elementdefault.nonpropagating;

/**
 * ElementDefaultAnnotatedTypeFactory calls addElementDefault on this package with
 * applyToSubpackages = false, defaulting FIELD locations to Bottom. The default applies here.
 */
public class InNonPropagating {
    Object f;

    void use() {
        // :: error: (assignment.type.incompatible)
        f = new Object();
    }
}
