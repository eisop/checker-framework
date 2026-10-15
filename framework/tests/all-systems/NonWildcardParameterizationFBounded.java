// Test case for the non-wildcard parameterization (JLS 9.9) of a functional interface type with a
// wildcard type argument for a type parameter whose bound mentions a type parameter of the same
// interface, such as the F-bounded NodeSupplier<T extends Node<T>>. JLS 9.9 leaves this case
// undefined; javac uses the bound of the wildcard, so the ground type of
// NodeSupplier<? extends Sub> is NodeSupplier<Sub>. Type argument inference used to compute
// NodeSupplier<Node<T>> instead, which mentions the interface's own type parameter T and made
// inference crash.

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class NonWildcardParameterizationFBounded {
    static class Node<T extends Node<T>> {
        T self() {
            throw new Error();
        }
    }

    static class Sub extends Node<Sub> {}

    interface NodeSupplier<T extends Node<T>> {
        T get();
    }

    interface NodeFunction<T extends Node<T>> {
        T apply(T t);
    }

    interface ListMaker<A, B extends List<A>> {
        B make(A a);
    }

    static <U> U supply(Supplier<U> s) {
        return s.get();
    }

    static <U> U applySub(Function<Sub, U> f) {
        throw new Error();
    }

    static <U> U withSupplier(U u, NodeSupplier<? extends Sub> s) {
        return u;
    }

    static <U> U withFunction(U u, NodeFunction<? extends Sub> f) {
        return u;
    }

    static <U> U withListMaker(U u, ListMaker<String, ? extends ArrayList<String>> m) {
        return u;
    }

    static Sub makeSub() {
        return new Sub();
    }

    static Sub identity(Sub s) {
        return s;
    }

    // A bound method reference whose receiver has a wildcard-parameterized type.
    Sub boundMethodReference(NodeSupplier<? extends Sub> s) {
        return supply(s::get);
    }

    // An unbound method reference to a method of the F-bounded class.
    Sub unboundMethodReference() {
        return applySub(Sub::self);
    }

    // Lambdas and method references whose target is a wildcard-parameterized functional interface
    // type.
    void targets() {
        String a = withSupplier("", () -> new Sub());
        String b = withSupplier("", NonWildcardParameterizationFBounded::makeSub);
        String c = withFunction("", x -> x);
        String d = withFunction("", NonWildcardParameterizationFBounded::identity);
        // The bound of B mentions the other type parameter A.
        String e = withListMaker("", x -> new ArrayList<>());
    }
}
