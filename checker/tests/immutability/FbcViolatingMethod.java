public class FbcViolatingMethod {
    FbcViolatingMethod() {
        // :: error: (method.invocation.invalid)
        foo();
    }

    void foo(FbcViolatingMethod this) {}
}
