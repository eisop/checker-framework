package packageannotatedfornullness;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Test {
    void foo(@Nullable Object o) {
        // :: error: (dereference.of.nullable)
        o.toString();
    }
}
