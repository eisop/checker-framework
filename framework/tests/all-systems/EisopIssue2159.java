// Type argument inference used to throw FalseBoundException on a constraint
// "capture of ? extends T <: C" for a non-generic class C, and the exception was reported as
// "type.argument.inference.crashed".  javac accepts this code.

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class EisopIssue2159 {
    static class Box<T> {}

    static <T> List<T> defer(Supplier<? extends List<? extends T>> s) {
        throw new Error();
    }

    static <T> Box<? extends Box<? extends T>> make() {
        throw new Error();
    }

    static <T> Function<Throwable, Box<? extends T>> makeFunction() {
        throw new Error();
    }

    void lambda() {
        List<? extends Number> x = defer(() -> new ArrayList<Integer>());
    }

    void doubleWildcard() {
        Box<? extends Box<? extends String>> b = make();
    }

    void function() {
        Function<Throwable, Box<? extends String>> f = makeFunction();
    }
}
