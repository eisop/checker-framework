// Type argument inference crashed with a NullPointerException, reported as
// "type.argument.inference.crashed", on an inexact method reference whose target function type
// returns void and whose compile-time declaration does not.  The target type must still mention an
// uninstantiated inference variable (`B` below), otherwise the constraint reduces trivially before
// the crashing code is reached.  javac accepts this code.

public class MethodRefVoidFunctionType {

    interface Sink<A, B> {
        void accept(A a);
    }

    static <X> int identity(X x) {
        return 0;
    }

    static <T, B> void run(T t, Sink<T, B> s) {}

    void test() {
        run(1, MethodRefVoidFunctionType::identity);
    }
}
