package afparent;

import org.jspecify.annotations.Nullable;

public class InParent {
    static void take(Object nn) {}

    void m(@Nullable Object nble) {
        // :: error: (argument.type.incompatible)
        take(nble);
    }
}

// @NullUnmarked on a class excludes it from the package's @AnnotatedFor scope.
@org.jspecify.annotations.NullUnmarked
class ExcludedFromParentPackage {
    void m(@Nullable Object nble) {
        InParent.take(nble);
    }
}
