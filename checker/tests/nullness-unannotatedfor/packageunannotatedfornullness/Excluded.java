package packageunannotatedfornullness;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.UnannotatedFor;

@UnannotatedFor("nullness")
public class Excluded {
    void foo(@Nullable Object o) {
        // No error: @UnannotatedFor excludes this class from the package's @AnnotatedFor scope.
        o.toString();
    }
}
