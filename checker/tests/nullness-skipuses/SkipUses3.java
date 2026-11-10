import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

public class SkipUses3 {

    static class SkipMe implements Iterable<Object> {
        @Override
        public Iterator<Object> iterator() {
            throw new UnsupportedOperationException();
        }
    }

    void iterate(@Nullable SkipMe s) {
        // The uses of SkipMe.iterator() are skipped, but not the check that the iterated
        // expression is non-null.
        // :: error: (iterating.over.nullable)
        for (Object o : s) {}
    }
}
