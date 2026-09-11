package uafoptout.sub;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * applyToSubpackages=false limits the exclusion to package uafoptout.sub itself; the package the
 * UnannotatedFor is written on is still excluded. So this code is outside package uafoptout's
 * AnnotatedFor scope and conservative defaults suppress its warnings. No error is expected below.
 */
public class InSubpackage {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
