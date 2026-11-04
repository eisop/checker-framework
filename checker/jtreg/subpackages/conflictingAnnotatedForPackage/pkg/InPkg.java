package pkg;

import org.checkerframework.checker.nullness.qual.Nullable;

// The package's AnnotatedFor won over its UnannotatedFor, so this code is checked.
public class InPkg {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
