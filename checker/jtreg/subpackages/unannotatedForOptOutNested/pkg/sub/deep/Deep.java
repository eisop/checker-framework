package pkg.sub.deep;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Package pkg.sub sets applyToSubpackages=false, which limits its own annotation to pkg.sub. It
 * does not block package pkg, whose AnnotatedFor applies to subpackages and so still reaches here,
 * and this code's warnings are issued.
 */
public class Deep {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
