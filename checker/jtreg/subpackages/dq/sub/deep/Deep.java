package dq.sub.deep;

/**
 * eisop#2037: package dq.sub's FIELD default (NonNull) shadows package dq's FIELD default
 * (Nullable) at dq.sub itself, and does not apply to subpackages -- but that must not remove dq's
 * own default for deeper subpackages, which nothing here shadows. This class must be defaulted with
 * dq's FIELD default (Nullable), not dq.sub's (NonNull), and not neither.
 */
public class Deep {
    Object f;

    void use() {
        f.toString();
    }
}
