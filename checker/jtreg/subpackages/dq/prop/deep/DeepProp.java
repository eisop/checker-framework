package dq.prop.deep;

/**
 * When an intervening package (dq.prop) shadows an enclosing package (dq)'s FIELD default with its
 * own FIELD default that has applyToSubpackages = true, that nearer default must win for deeper
 * subpackages as well: this class must be defaulted with dq.prop's FIELD default (NonNull), not
 * dq's (Nullable).
 */
public class DeepProp {
    Object f = new Object();

    void use() {
        f.toString();
        f = null;
    }
}
