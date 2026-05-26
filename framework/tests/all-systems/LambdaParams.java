import java.lang.reflect.Method;
import java.util.function.Function;

public class LambdaParams {
    interface Stream<T> {
        <S> Stream<S> map(Function<? super T, ? extends S> mapper);
    }

    static <Z> Z identity(Z p) {
        throw new RuntimeException();
    }

    static <Q> Stream<Q> stream(Q[] array) {
        throw new RuntimeException();
    }

    // TODO: inference results in invalid annotations for some type systems.
    // What is a better solution?
    @SuppressWarnings("type.invalid.annotations.on.location")
    static void method() {
        Function<? super Method, ? extends Stream<? extends String>> mapper =
                identity(f -> stream(f.getAnnotations()).map(annotation -> ""));
    }
}
