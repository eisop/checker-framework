package nullmarkedpkg;

import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

public class InPackage {
    static void take(Object nn) {}

    void m(@Nullable Object nble) {
        // :: error: (argument.type.incompatible)
        take(nble);
    }
}

// @NullUnmarked excludes this class from the package's @NullMarked scope.
@NullUnmarked
class ExcludedFromPackage {
    void m(@Nullable Object nble) {
        InPackage.take(nble);
    }
}
