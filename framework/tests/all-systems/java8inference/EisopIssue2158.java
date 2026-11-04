// Test case for https://github.com/eisop/checker-framework/issues/2158
// A generic method call returned by a lambda whose target is a raw functional interface type.
// The return type of the function type of a raw Supplier is a type argument of a raw type, which
// was used as the target type of never() and made inference crash.  For a cast, the functional
// interface type was additionally computed as if the cast type were an expression.

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EisopIssue2158 {
    static <T> List<T> never() {
        throw new Error();
    }

    static final Supplier ZERO_SUPPLIER = () -> never();

    static final Function NEVER = e -> never();

    void m(List raw) {
        Supplier s = () -> never();
        Supplier block =
                () -> {
                    return never();
                };
        Object cast = (Supplier) () -> never();
        Function f = x -> x.hashCode();
        raw.add(never());
        raw.add(new ArrayList<>());
    }
}
