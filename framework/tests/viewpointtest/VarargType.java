import viewpointtest.quals.*;

public class VarargType {

    VarargType(String str, Object... args) {}

    void foo() {
        VarargType a = new VarargType("testStr", new Object());
    }

    // inner class
    class Inner {
        Inner(String str, Object... args) {}

        void foo() {
            Inner a = new Inner("testStr", new Object());
        }
    }

    // anonymous class
    Object o =
            new Object() {
                void foo() {
                    VarargType a = new VarargType("testStr", new Object());
                }
            };
}
