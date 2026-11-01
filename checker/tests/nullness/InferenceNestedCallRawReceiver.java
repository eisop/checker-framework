import java.util.NavigableSet;
import java.util.TreeSet;

// Type argument inference crashed for newTreeSet(): the invocation of put has no type variables,
// because its receiver is raw, so inferring put did not infer the type arguments of its nested
// arguments. newTreeSet() was then inferred on its own against the parameter type of unmod before
// unmod's type arguments were inferred, which treated unmod's type variable E as a proper type.
public class InferenceNestedCallRawReceiver {
    @SuppressWarnings("rawtypes")
    static <E extends Comparable> TreeSet<E> newTreeSet() {
        return new TreeSet<>();
    }

    static <E> NavigableSet<E> unmod(NavigableSet<E> s) {
        return s;
    }

    static class Builder<B> {
        <T extends B> Builder<B> put(Class<T> key, T value) {
            return this;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void rawReceiver(Builder b) {
        b.put(NavigableSet.class, unmod(newTreeSet()));
    }
}
