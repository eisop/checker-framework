import org.checkerframework.checker.immutability.qual.Readonly;

public class Transitive {

    // class A, B, C are not annotated to test transitive mutability by default.

    static class A {
        B b;

        public B getB() {
            return b;
        }
    }

    static class B {
        int field = 0;
        C c;

        public C getC() {
            return c;
        }
    }

    static class C {
        int field = 0;
    }

    static class Caller {
        void test(@Readonly A a) {
            // :: error: (illegal.field.write)
            a.b.field = 1;
            // :: error: (method.invocation.invalid)
            a.getB().field = 1;

            // :: error: (illegal.field.write)
            a.b.c.field = 1;
            // :: error: (method.invocation.invalid)
            a.getB().getC().field = 1;
            // :: error: (method.invocation.invalid)
            a.b.getC().field = 1;
            // :: error: (method.invocation.invalid)
            a.getB().c.field = 1;
        }
    }
}
