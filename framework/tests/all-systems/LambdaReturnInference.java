// Type argument inference used to throw FalseBoundException ("T[] <: R") on a generic call
// returned by a lambda that is an argument to a generic method, when the call's argument is a new
// array: finding the new array's qualifiers restarted inference of arr(...) while run(...) was
// still inferring it, against run's not-yet-inferred R.  javac accepts this code.
// https://github.com/eisop/checker-framework/issues/2086

import java.util.List;
import java.util.function.Supplier;

public class LambdaReturnInference {

    static <R> R run(Supplier<R> s) {
        throw new Error();
    }

    static <T> T[] arr(T[] a) {
        return a;
    }

    static <T> List<T> lst(T[] a) {
        throw new Error();
    }

    String[] expressionBody() {
        return run(() -> arr(new String[0]));
    }

    String[] blockBody() {
        return run(
                () -> {
                    return arr(new String[0]);
                });
    }

    String[] nestedReturn(boolean b) {
        return run(
                () -> {
                    if (b) {
                        return arr(new String[0]);
                    }
                    return arr(new String[] {});
                });
    }

    List<String> list() {
        return run(() -> lst(new String[0]));
    }

    String[] nestedLambda() {
        return run(() -> run(() -> arr(new String[0])));
    }

    void notGeneric() {
        Supplier<String[]> s = () -> arr(new String[0]);
    }
}
