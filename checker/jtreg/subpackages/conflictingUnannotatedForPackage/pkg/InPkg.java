package pkg;

import org.checkerframework.checker.nullness.qual.Nullable;

// The package's UnannotatedFor was written first, so it won and this code is not checked: the
// argument below would otherwise be an argument.type.incompatible error.
public class InPkg {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
