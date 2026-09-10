package nup.sub.deep;

import org.jspecify.annotations.Nullable;

// The NullUnmarked alias sets applyToSubpackages=false, matching JSpecify, so it does not exclude
// this nested subpackage. Package nup's AnnotatedFor applies to subpackages and still reaches
// here, so this code is checked and its warnings are issued.
public class Deep {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
