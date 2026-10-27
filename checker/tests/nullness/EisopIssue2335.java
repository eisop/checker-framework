// Test case for https://github.com/eisop/checker-framework/issues/2335 :
// type argument inference crashed on a lambda or method reference whose functional interface
// declares `throws E` and whose target type instantiates E with a class.

class EisopIssue2335 {
    interface ThrowingSupplier<E extends Throwable> {
        String get() throws E;
    }

    static <T> T parse(ThrowingSupplier<Exception> parser) {
        throw new Error();
    }

    static <T, E extends Throwable> T parseE(ThrowingSupplier<E> parser) throws E {
        throw new Error();
    }

    static String throwing() throws Exception {
        return "x";
    }

    String lambda() {
        return parse(() -> "x");
    }

    String methodReference() {
        return parse(EisopIssue2335::throwing);
    }

    String typeVariableThrown() throws Exception {
        return parseE(EisopIssue2335::throwing);
    }
}
