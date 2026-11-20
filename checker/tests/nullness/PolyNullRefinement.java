// Where a @PolyNull expression has been found to be null, the invocation instantiates @PolyNull as
// @Nullable, and dataflow gives a @Nullable value the @PolyNull form, so it is compatible with a
// @PolyNull variable,
// parameter, or return type. The visitor compares the types as computed, without changing them.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

class PolyNullRefinement {
    static @PolyNull String id2(@PolyNull String a, @PolyNull String b) {
        return a;
    }

    @PolyNull String assignThenReturn(@PolyNull String param) {
        if (param == null) {
            param = null;
        }
        return param;
    }

    @PolyNull String assignNullableThenReturn(@PolyNull String param, @Nullable String n) {
        if (param == null) {
            param = n;
        }
        return param;
    }

    @PolyNull String conditional(@PolyNull String p) {
        return p == null ? null : p.trim();
    }

    // The else operand is non-null whatever @PolyNull is instantiated as.
    @PolyNull String conditionalOtherParam(@PolyNull String p, @PolyNull String r) {
        return p == null ? null : "x";
    }

    @PolyNull String conditionalAssignment(@PolyNull String p, @Nullable String n) {
        @PolyNull String result = p == null ? n : p;
        result = p != null ? p : null;
        return result;
    }

    // That every @PolyNull parameter is currently non-null does not show that every @PolyNull value
    // is non-null: a reassigned parameter no longer holds its argument, so r can still be null.
    @PolyNull String polyLocal(@PolyNull String p, @PolyNull String q) {
        @PolyNull String r = q;
        q = "";
        if (p != null) {
            // :: error: (dereference.of.nullable)
            r.toString();
        }
        return r;
    }

    @PolyNull String argument(@PolyNull String p, @Nullable String n) {
        if (p == null) {
            @PolyNull String r = id2(p, null);
            r = id2(p, n);
            return id2(n, null);
        }
        return id2(p, p);
    }

    @PolyNull String stillErrors(@PolyNull String p, @Nullable String n) {
        if (p != null) {
            // :: error: (assignment.type.incompatible)
            p = null;
            // :: error: (return.type.incompatible)
            return n;
        }
        // Only the p == null path reaches here, where @PolyNull is instantiated as @Nullable.
        return n;
    }

    @PolyNull String noComparison(@PolyNull String p, @Nullable String n) {
        // :: error: (return.type.incompatible)
        return n;
    }

    // @PolyNull is instantiated as @Nullable here, so a @Nullable value is still not @NonNull.
    void nonNullTarget(@PolyNull String p, @Nullable String n) {
        if (p == null) {
            // :: error: (assignment.type.incompatible)
            @NonNull String s = null;
            // :: error: (assignment.type.incompatible)
            @NonNull String t = n;
            // :: error: (dereference.of.nullable)
            n.toString();
            // :: error: (dereference.of.nullable)
            p.toString();
        }
    }
}
