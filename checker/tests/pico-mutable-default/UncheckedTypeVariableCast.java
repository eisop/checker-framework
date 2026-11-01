import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;

// An unchecked cast to a type variable is trusted, as javac trusts it: (E) o is E whatever the
// qualifier of o, as in a collection that stores its elements in an Object[].
class UncheckedTypeVariableCast<E extends @Readonly Object> {
    @SuppressWarnings("unchecked")
    E fromArray(Object[] elements) {
        E e = (E) elements[0];
        return e;
    }

    @SuppressWarnings("unchecked")
    E fromMutable(@Mutable Object o) {
        return (E) o;
    }

    @SuppressWarnings("unchecked")
    E fromImmutable(@Immutable Object o) {
        return (E) o;
    }

    @SuppressWarnings("unchecked")
    void asArgument(@Readonly Object o) {
        take((E) o);
    }

    void take(E e) {}
}
