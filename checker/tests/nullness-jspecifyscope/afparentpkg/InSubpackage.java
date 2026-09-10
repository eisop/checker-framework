package afparent.sub;

import org.jspecify.annotations.Nullable;

// afparent's @AnnotatedFor applies to subpackages, so this code is checked. (A @NullUnmarked
// package-info for afparent.sub cannot live in this directory -- one directory holds one
// package-info -- so the package-level @NullUnmarked case is a jtreg test instead; see
// checker/jtreg/subpackages/NullUnmarkedPackage.java.)
public class InSubpackage {
    static void take(Object nn) {}

    void m(@Nullable Object nble) {
        // :: error: (argument.type.incompatible)
        take(nble);
    }
}
