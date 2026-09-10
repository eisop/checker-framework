package uafoptout.sub.deep;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Package uafoptout.sub sets applyToSubpackages=false, which limits its own annotation to
 * uafoptout.sub. It does not block package uafoptout, whose AnnotatedFor applies to subpackages and
 * so still reaches here, and this code's warnings are issued.
 */
public class Deep {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
