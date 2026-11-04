package elementdefault.pkg.sub;

/**
 * This subpackage has no default of its own. The FIELD default that
 * ElementDefaultAnnotatedTypeFactory added programmatically on package elementdefault.pkg (via
 * addElementDefault, not a written @DefaultQualifier) must still reach here, the same way a written
 * default would -- this is what eisop#2037's fix, and the addElementDefault code path it touches,
 * are responsible for.
 */
public class InSub {
    Object f;

    void use() {
        // :: error: (assignment.type.incompatible)
        f = new Object();
    }
}
