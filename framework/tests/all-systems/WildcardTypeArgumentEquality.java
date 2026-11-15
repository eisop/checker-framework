// Wildcard type arguments of an invariant type are compared through their captures, so an
// unbounded wildcard equals a wildcard whose extends bound is the declared bound of the type
// parameter, and wildcards with nested type arguments or a super bound compare without a crash.

import java.util.List;
import java.util.Map;

public class WildcardTypeArgumentEquality {
    static class Box<T> {
        T t;

        Box(T t) {
            this.t = t;
        }
    }

    interface A<T> {}

    static class Bounded<T extends A<?>> {}

    static class Recursive<T extends Comparable<T>> {}

    void unboundedWildcard(Box<Bounded<?>> b) {
        Box<Bounded<? extends A<?>>> c = b;
        Box<Bounded<?>> d = c;
    }

    void recursiveBound(Box<Recursive<?>> b) {
        Box<Recursive<? extends Comparable<?>>> c = b;
        Box<Recursive<?>> d = c;
    }

    void superBound(Box<List<? super String>> b) {
        Box<List<? super String>> c = b;
    }

    void nestedTypeArguments(Box<Map<? extends List<String>, ? super List<String>>> b) {
        Box<Map<? extends List<String>, ? super List<String>>> c = b;
    }
}
