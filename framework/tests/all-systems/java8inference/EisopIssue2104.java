// Test case for https://github.com/eisop/checker-framework/issues/2104
// The checked exceptions thrown by a lambda body were computed twice: once from the declared
// thrown types of the invoked methods, and once from their thrown types after type argument
// inference. For a nested call whose thrown type variable is inferred to be RuntimeException, the
// two lists differed in length, and inference crashed with a NoSuchElementException.

public class EisopIssue2104 {
    interface TR<E extends Exception> {
        void run() throws E;
    }

    static <E extends Exception> void call(TR<E> r) throws E {
        r.run();
    }

    static void checked() throws Exception {}

    void nested() throws Exception {
        call(() -> call(() -> {}));
    }

    void nestedBlock() throws Exception {
        call(
                () -> {
                    call(() -> {});
                });
    }

    void nestedChecked() throws Exception {
        call(() -> call(() -> checked()));
    }

    void twoLevels() throws Exception {
        call(() -> call(() -> call(() -> {})));
    }
}
