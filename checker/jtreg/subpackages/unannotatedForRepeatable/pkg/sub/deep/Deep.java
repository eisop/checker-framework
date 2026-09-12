package pkg.sub.deep;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

// Package pkg.sub's two UnannotatedFor entries apply independently: nullness set
// applyToSubpackages=true, so this subpackage stays excluded; index set applyToSubpackages=false,
// so package pkg's AnnotatedFor reaches through and the Index Checker reports here.
public class Deep {
    void takeNonNull(Object nn) {}

    void nullness(@Nullable Object n) {
        takeNonNull(n);
    }

    void takeNonNegative(@NonNegative int i) {}

    void index(int i) {
        takeNonNegative(i);
    }
}
