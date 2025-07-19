/*
 * @test
 * @summary Test case for method receiver type in error message. If it is a non-raw type, it should be printed as such.
 *
 * @compile/fail/ref=MethodReceiverTypeErrorMessageTest.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker MethodReceiverTypeErrorMessageTest.java
 */
public class MethodReceiverTypeErrorMessageTest<T> {

    MethodReceiverTypeErrorMessageTest() {
        foo();
    }

    void foo() {}
}

class sub extends MethodReceiverTypeErrorMessageTest<String> {
    sub() {
        foo();
    }
}
