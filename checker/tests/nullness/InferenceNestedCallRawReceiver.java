import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Supplier;

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

    static <E> List<E> listOf(Collection<E> c) {
        return new ArrayList<>(c);
    }

    static class Builder<B> {
        <T extends B> Builder<B> put(Class<T> key, T value) {
            return this;
        }

        Builder<B> putObject(Object value) {
            return this;
        }
    }

    static class Box<T> {
        Box(T t) {}
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void rawReceiver(Builder b) {
        b.put(NavigableSet.class, unmod(newTreeSet()));
        // More than one generic invocation between put and newTreeSet().
        b.put(NavigableSet.class, unmod(unmod(newTreeSet())));
        b.put(List.class, listOf(unmod(newTreeSet())));
        // A diamond constructor invocation between them.
        b.put(Box.class, new Box<>(unmod(newTreeSet())));
        // A lambda between them.
        b.put(Supplier.class, (Supplier) () -> unmod(newTreeSet()));
    }

    // A method without type variables of a parameterized receiver.
    void nonGenericMethod(Builder<Object> b) {
        b.putObject(unmod(newTreeSet()));
    }
}
