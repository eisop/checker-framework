public final class ExceptionInLambdaTest {

    public static void exceptionInLambda() {
        try {
            runLambda(
                    new Object(),
                    obj -> {
                        throw new Exception();
                    });
        } catch (Exception e) {
        }
    }

    public static <T, E extends Throwable> void runLambda(T t, ConsumerWithException<T, E> lambda)
            throws E {
        lambda.run(t);
    }

    public static interface ConsumerWithException<T, E extends Throwable> {

        void run(T element) throws E;
    }
}
