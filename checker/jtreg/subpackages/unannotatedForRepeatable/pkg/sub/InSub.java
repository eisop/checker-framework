package pkg.sub;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

// Own package: both UnannotatedFor entries exclude this class from package pkg's
// AnnotatedFor scope, whatever their applyToSubpackages says, so neither checker reports here.
public class InSub {
    void takeNonNull(Object nn) {}

    void nullness(@Nullable Object n) {
        takeNonNull(n);
    }

    void takeNonNegative(@NonNegative int i) {}

    void index(int i) {
        takeNonNegative(i);
    }
}
