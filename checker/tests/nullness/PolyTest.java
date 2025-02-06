import org.checkerframework.checker.nullness.qual.PolyNull;

class PolyTest {
    void foo1(@PolyNull Object nbl) {
        if (nbl == null) {
            // :: error: (dereference.of.nullable)
            nbl.toString();
        }
    }

    void foo2(@PolyNull Object nbl) {
        // :: error: (dereference.of.nullable)
        nbl.toString();
    }
}
