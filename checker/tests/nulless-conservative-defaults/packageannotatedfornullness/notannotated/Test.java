package packageannotatedfornullness.notannotated;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Test {
    void foo(@Nullable Object o) {
        // No error because this package is not annotated for nullness.
        o.toString();
    }
}
