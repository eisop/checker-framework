package elementdefault.nonpropagating.sub;

/**
 * The FIELD default that ElementDefaultAnnotatedTypeFactory added with addPackageDefault on package
 * elementdefault.nonpropagating with applyToSubpackages = false must not reach this subpackage, so
 * the field has the checked-code default, Top.
 */
public class InNonPropagatingSub {
    Object f;

    void use() {
        f = new Object();
    }
}
