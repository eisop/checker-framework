public class VarargType {
    void foo() {
        TestClass a = new TestClass("testStr", new Object());
    }
}

class TestClass {
    TestClass(String str, Object... args) {}
}
