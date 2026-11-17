package permissivedefaultslib;

import org.checkerframework.checker.index.qual.Positive;

/** Unchecked code: not {@code @AnnotatedFor("value")}. */
public class ValueLib {
    public @Positive int positive() {
        return 1;
    }
}
