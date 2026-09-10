package uafoptout.sub;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Opting out of subpackages does not opt the annotated package itself out, so this code is excluded
 * from package uafoptout's AnnotatedFor scope and conservative defaults suppress its warnings. No
 * error is expected below.
 */
public class InSubpackage {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
