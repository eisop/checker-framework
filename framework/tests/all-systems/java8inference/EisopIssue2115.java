// Test case for https://github.com/eisop/checker-framework/issues/2115
// A subclass of Error is an unchecked exception (JLS 11.1.1). Type argument inference treated an
// Error thrown in a lambda body as a checked exception and constrained the functional interface's
// thrown type variable by it, which crashed inference.

public class EisopIssue2115 {
    static class MyError extends Error {}

    interface Task<E extends RuntimeException> {
        void run() throws E;
    }

    static <E extends RuntimeException> void invoke(Task<E> t) {
        t.run();
    }

    static void fail() throws MyError {
        throw new MyError();
    }

    void throwStatement() {
        invoke(
                () -> {
                    throw new MyError();
                });
    }

    void assertionError() {
        invoke(
                () -> {
                    throw new AssertionError();
                });
    }

    void declaredThrows() {
        invoke(() -> fail());
    }

    void constructor() {
        invoke(
                () -> {
                    new Thrower();
                });
    }

    static class Thrower {
        Thrower() throws MyError {}
    }
}
