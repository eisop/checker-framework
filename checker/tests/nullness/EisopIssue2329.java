// Test case for https://github.com/eisop/checker-framework/issues/2329
// A generic method reference returned by a lambda whose target is a raw functional interface type.

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EisopIssue2329 {
    interface S2<T extends Supplier<List<String>>> {
        T get();
    }

    static class Box<T extends Supplier<List<String>>> {
        Box() {}

        Box(T t) {}

        void set(T t) {}
    }

    static <E> List<E> glist() {
        throw new Error();
    }

    static List<String> list() {
        throw new Error();
    }

    void returnedByLambdaWithRawTarget() {
        S2 n = () -> EisopIssue2329::glist;
        S2 a = () -> ArrayList::new;
        S2 block =
                () -> {
                    return EisopIssue2329::glist;
                };
        Object cast = (S2) () -> EisopIssue2329::glist;
    }

    // Controls that did not crash.
    void controls(Box raw) {
        S2<Supplier<List<String>>> parameterized = () -> EisopIssue2329::glist;
        S2 nonGeneric = () -> EisopIssue2329::list;
        S2 lambda = () -> () -> glist();
        raw.set(EisopIssue2329::glist);
        raw.set(ArrayList::new);
        new Box(EisopIssue2329::glist);
    }
}
