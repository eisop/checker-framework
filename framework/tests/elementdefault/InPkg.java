package elementdefault.pkg;

/**
 * ElementDefaultAnnotatedTypeFactory calls addElementDefault directly on this package, defaulting
 * FIELD locations to Bottom. An unqualified value is not assignable to a Bottom-defaulted field.
 */
public class InPkg {
    Object f;

    void use() {
        // :: error: (assignment.type.incompatible)
        f = new Object();
    }
}
