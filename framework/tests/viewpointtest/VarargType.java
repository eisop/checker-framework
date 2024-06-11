public class VarargType {
    void foo() {
        A a = new A("testStr", new Object());
    }
}

class A {
    A(String str, Object... args) {}
}
