import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Suppression {

    Object f;

    @SuppressWarnings("nullnessnoinit")
    void test1() {
        String a = null;
        a.toString();
        String nonkey = "";
        @KeyFor("map") String key = nonkey;
    }

    @SuppressWarnings("nullnesskeyfor")
    void test2() {
        String a = null;
        a.toString();
        String nonkey = "";
        @KeyFor("map") String key = nonkey;
    }

    @SuppressWarnings("initialization")
    void test3() {
        String a = null;
        // :: error: (dereference.of.nullable)
        a.toString();
    }

    @SuppressWarnings("nullness")
    Suppression(@Nullable Object o) {
        o.toString();
        String nonkey = "";
        @KeyFor("map") String key = nonkey;
    }

    @SuppressWarnings("nullnesskeyfor")
    // :: error: (initialization.fields.uninitialized)
    Suppression(@Nullable Object o, int dummy) {
        o.toString();
        String nonkey = "";
        @KeyFor("map") String key = nonkey;
    }

    @SuppressWarnings("nullnessinitialization")
    Suppression(@Nullable Object o, int dummy1, int dummy2) {
        o.toString();
        String nonkey = "";
        // :: error: (assignment.type.incompatible) :: error: (expression.unparsable.type.invalid)
        @KeyFor("map") String key = nonkey;
    }

    @SuppressWarnings("nullnessonly")
    // :: error: (initialization.fields.uninitialized)
    Suppression(@Nullable Object o, int dummy1, int dummy2, int dummy3) {
        o.toString();
        String nonkey = "";
        // :: error: (assignment.type.incompatible) :: error: (expression.unparsable.type.invalid)
        @KeyFor("map") String key = nonkey;
    }

    @SuppressWarnings("keyfor")
    // :: error: (initialization.fields.uninitialized)
    Suppression(@Nullable Object o, int dummy1, int dummy2, int dummy3, int dummy4) {
        // :: error: (dereference.of.nullable)
        o.toString();
        String nonkey = "";
        @KeyFor("map") String key = nonkey;
    }

    @SuppressWarnings("initialization")
    Suppression(@Nullable Object o, int dummy1, int dummy2, int dummy3, int dummy4, int dummy5) {
        // :: error: (dereference.of.nullable)
        o.toString();
        String nonkey = "";
        // :: error: (assignment.type.incompatible) :: error: (expression.unparsable.type.invalid)
        @KeyFor("map") String key = nonkey;
    }
}
