// Test case for typetools issue #6374:
// https://github.com/typetools/checker-framework/issues/6374
// Also see checker/jtreg/nullness/issue6374/

import org.checkerframework.checker.nullness.qual.Nullable;
import org.jmlspecs.annotation.NonNull;

public class Issue6374 {

    @SuppressWarnings("unchecked") // ignore heap pollution
    static class Lib {
        // element type inferred, array non-null
        static <T> void none(T... o) {}

        // element type inferred, array non-null
        static <T> void decl(@NonNull T... o) {}

        // element type nullable, array non-null
        static <T> void type(@Nullable T... o) {}

        // element type nullable, array nullable
        static <T> void typenn(@Nullable T @Nullable ... o) {}
    }

    class User {
        void go() {
            Lib.decl("", null);
            // :: error: (argument.type.incompatible)
            Lib.decl((Object[]) null);
            Lib.type("", null);
            // :: error: (argument.type.incompatible)
            Lib.type((Object[]) null);
            Lib.typenn("", null);
            Lib.typenn((Object[]) null);
        }
    }
}
