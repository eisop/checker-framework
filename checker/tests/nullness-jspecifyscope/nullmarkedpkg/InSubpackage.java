package nullmarkedpkg.sub;

import org.jspecify.annotations.Nullable;

// This file is deliberately in the same directory as nullmarkedpkg/package-info.java even though
// it declares a subpackage: the test harness compiles each directory separately, so a file in a
// sub-directory would not see that package-info and would assert nothing.
//
// A @NullMarked package does not mark its subpackages, so the alias sets applyToSubpackages=false
// and this code is outside any @AnnotatedFor scope. No error is expected below.
public class InSubpackage {
    static void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
