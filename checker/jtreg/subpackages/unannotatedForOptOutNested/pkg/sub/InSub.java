package pkg.sub;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * applyToSubpackages=false limits the exclusion to package pkg.sub itself; the package the
 * UnannotatedFor is written on is still excluded. So this code is outside package pkg's
 * AnnotatedFor scope and conservative defaults suppress its warnings. No error is expected below.
 */
public class InSub {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
