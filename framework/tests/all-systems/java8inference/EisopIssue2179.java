// Test case for https://github.com/eisop/checker-framework/issues/2179
// A generic method call returned by a lambda that is itself returned by a lambda whose target is a
// raw functional interface type.  The target of the inner lambda is a type argument of a raw type,
// whose function type was computed with the unsubstituted type variables of the inner functional
// interface.  The type variable T of Supplier was then used as the target type of never() and made
// inference crash.

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EisopIssue2179 {
    interface S2<T extends Supplier<List<String>>> {
        T get();
    }

    interface F2<T extends Function<String, List<String>>> {
        T get();
    }

    static <T> List<T> never() {
        throw new Error();
    }

    static List<String> list() {
        throw new Error();
    }

    static final S2 FIELD = () -> () -> never();

    void m() {
        S2 s = () -> () -> never();
        S2 block =
                () -> {
                    return () -> {
                        return never();
                    };
                };
        Object cast = (S2) () -> () -> never();
        S2 ref = () -> EisopIssue2179::list;
        S2 str = () -> () -> "x";
        F2 f = () -> x -> never();
        F2 hash = () -> x -> x.hashCode();
    }
}
