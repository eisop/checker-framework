package uaf.sub.deep;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Package uaf.sub is UnannotatedFor, and applyToSubpackages defaults to true, so its exclusion
 * reaches here even though package uaf is AnnotatedFor. This code is therefore outside any
 * AnnotatedFor scope and conservative defaults suppress its warnings; no error is expected below.
 */
public class Deep {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
