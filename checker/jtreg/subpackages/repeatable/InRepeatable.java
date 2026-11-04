package repeatable;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The package is written for two checkers at once, each with its own applyToSubpackages: writing
 * two @AnnotatedFor at the same location (rather than listing both checkers in one) is what
 * requires @AnnotatedFor to be @Repeatable. Own package: both checkers are always in scope here,
 * whatever their applyToSubpackages says.
 */
public class InRepeatable {
    void takeNonNull(Object nn) {}

    void nullness(@Nullable Object n) {
        takeNonNull(n);
    }

    void takeNonNegative(@NonNegative int i) {}

    void index(int i) {
        takeNonNegative(i);
    }
}
