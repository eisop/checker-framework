// Test case for https://github.com/eisop/checker-framework/issues/2098
// The return type of P::type, Class<? extends T>, is captured, and the type argument of the outer
// call depends on the resulting capture variable.  It was resolved before the capture variable,
// so providers.map(P::type) was inferred as Stream<Object>, and inference of the chained call
// crashed.

import java.util.stream.Stream;

public class EisopIssue2098 {
    interface P<T> {
        Class<? extends T> type();
    }

    interface Box<X> {
        String name();
    }

    interface Q<T> {
        Box<? extends T> box();
    }

    Stream<String> methodRef(Stream<P<Object>> providers) {
        return providers.map(P::type).map(Class::getName);
    }

    Stream<String> lambda(Stream<P<Object>> providers) {
        return providers.map(P::type).map(c -> c.getName());
    }

    Stream<String> string(Stream<P<String>> providers) {
        return providers.map(P::type).map(Class::getName);
    }

    Stream<String> userDefined(Stream<Q<Object>> providers) {
        return providers.map(Q::box).map(Box::name);
    }

    Stream<Class<? extends Object>> single(Stream<P<Object>> providers) {
        return providers.map(P::type);
    }
}
