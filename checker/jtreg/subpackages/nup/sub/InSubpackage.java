package nup.sub;

import org.jspecify.annotations.Nullable;

// JSpecify's NullUnmarked is aliased to UnannotatedFor("nullness"), so it excludes this package
// from package nup's AnnotatedFor scope. No error is expected below.
public class InSubpackage {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
