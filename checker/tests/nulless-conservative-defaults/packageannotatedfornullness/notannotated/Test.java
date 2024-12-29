package packageannotatedfornullness.notannotated;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Test {
    void foo(@Nullable Object o) {
        o.toString();
    }
}
