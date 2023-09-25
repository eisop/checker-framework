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
}
