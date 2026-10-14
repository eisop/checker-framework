// A constraint "capture of ? extends T <: C" is reduced against the capture variable's upper bound
// T, so qualifiers on C still constrain T.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class EisopIssue2159 {
    static class Box<T> {}

    static <T> Box<? extends Box<? extends T>> make(T t) {
        throw new Error();
    }

    static <T> Function<Throwable, Box<? extends T>> makeFunction(T t) {
        throw new Error();
    }

    static <T> List<T> defer(Supplier<? extends List<? extends T>> s) {
        throw new Error();
    }

    void m(String s, @Nullable String ns, List<String> l, List<@Nullable String> nl) {
        Box<? extends Box<? extends String>> b1 = make(s);
        // :: error: (assignment.type.incompatible) :: error: (type.arguments.not.inferred)
        Box<? extends Box<? extends String>> b2 = make(ns);

        Function<Throwable, Box<? extends String>> f1 = makeFunction(s);
        // :: error: (assignment.type.incompatible) :: error: (type.arguments.not.inferred)
        Function<Throwable, Box<? extends String>> f2 = makeFunction(ns);

        List<? extends String> l1 = defer(() -> l);
        // :: error: (assignment.type.incompatible) :: error: (type.arguments.not.inferred)
        List<? extends String> l2 = defer(() -> nl);
    }
}
