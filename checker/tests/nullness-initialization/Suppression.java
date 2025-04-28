import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Suppression {

    Object f;

    @SuppressWarnings("nullnessnoinit")
    void test() {
        String a = null;
        a.toString();
    }

    @SuppressWarnings("initialization")
    void test2() {
        String a = null;
        // :: error: (dereference.of.nullable)
        a.toString();
    }

    @SuppressWarnings("nullness")
    Suppression() {}

    @SuppressWarnings("nullnessnoinit")
    // :: error: (initialization.fields.uninitialized)
    Suppression(int dummy) {}

    @SuppressWarnings("nullnessnokeyfor")
    Suppression(int dummy, @Nullable Object o) {
        f = o;
        o.toString();
    }

    @SuppressWarnings("nullnessnokeyfor")
    Suppression(int dummy, @Nullable Object o, Object o2) {
        f = o;
        o2.toString();
        String nonkey = "";
        // :: error: (assignment.type.incompatible) :: error: (expression.unparsable.type.invalid)
        @KeyFor("map") String key = nonkey;
    }
}
