package packageunannotatedfornullness;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Included {
    void foo(@Nullable Object o) {
        // :: error: (dereference.of.nullable)
        o.toString();
    }
}
