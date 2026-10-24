import org.checkerframework.checker.nullness.qual.*;

// A finally block is copied into the control flow graph once per way of leaving the try block, so
// one tree has several nodes. The cached type of a tree must not be served from the node of
// another copy or of an earlier pass over a loop (see Issue602).
public class FlowDependentFinally {
    static @PolyNull Object pid(@PolyNull Object o) { return o; }
    static <T> T bar() { throw new Error(); }
    static <T extends @Nullable Object> T same(T a, T b) { return a; }
    static void mayThrow() {}

    void a(@Nullable Object y, boolean c) {
        Object x = "";
        while (c) {
            try {
                x = y;
                mayThrow();
                x = "";
            } finally {
                // :: error: (dereference.of.nullable)
                same(pid(x), bar()).toString();
            }
        }
    }

    void b(@Nullable Object y, boolean c) {
        Object x = y;
        while (c) {
            try {
                x = "";
                mayThrow();
                x = y;
            } finally {
                // :: error: (dereference.of.nullable)
                same(pid(x), bar()).toString();
            }
        }
    }

    Object c(@Nullable Object y, boolean c) {
        Object x = "";
        while (c) {
            try {
                if (c) { x = ""; return ""; }
                if (c) { x = ""; continue; }
                if (c) { x = ""; break; }
                x = ""; mayThrow(); x = y;
            } finally {
                // :: error: (dereference.of.nullable)
                same(pid(x), bar()).toString();
            }
        }
        return "";
    }

    Object d(@Nullable Object y, boolean c) {
        Object x = "";
        while (c) {
            try {
                if (c) { return ""; }
                if (c) { continue; }
                if (c) { break; }
                mayThrow();
            } finally {
                // :: error: (dereference.of.nullable)
                same(pid(x), bar()).toString();
                x = y;
            }
        }
        return "";
    }

    // the finally copy itself loops
    void e(@Nullable Object y, boolean c) {
        Object x = "";
        try {
            mayThrow();
        } finally {
            while (c) {
                // :: error: (dereference.of.nullable)
                same(pid(x), bar()).toString();
                x = y;
            }
        }
    }
}
