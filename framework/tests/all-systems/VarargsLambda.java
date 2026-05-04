import org.checkerframework.common.aliasing.qual.NonLeaked;

import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("all") // Just check for crashes.
public class VarargsLambda {

    // ----------------------------------------------------------------------
    // The minimal reproducer: two lambdas in a Consumer<String>... vararg.
    @SafeVarargs
    static void runAll(int prefixArg, Consumer<String>... consumers) {
        for (Consumer<String> c : consumers) {
            c.accept("hello");
        }
    }

    void minimalReproducer() {
        // Before the fix: analysis crashed with IndexOutOfBoundsException
        // while building the initial store for the second lambda.
        runAll(0, s -> {}, t -> {});
    }

    // ----------------------------------------------------------------------
    // Three lambdas — exercises the loop more.
    void threeLambdas() {
        runAll(0, s -> {}, s -> {}, s -> {});
    }

    // ----------------------------------------------------------------------
    // One lambda in a varargs slot — the boundary case (index == formals - 1)
    // that was already handled before the fix, but is worth confirming still
    // works after the change.
    void oneLambdaInVarargs() {
        runAll(0, s -> {});
    }

    // ----------------------------------------------------------------------
    // Mixed: a non-lambda argument plus multiple lambdas in the varargs.
    @SafeVarargs
    static void mixedRunner(String header, Consumer<String> first, Consumer<String>... rest) {
        first.accept(header);
        for (Consumer<String> r : rest) {
            r.accept(header);
        }
    }

    void mixedFormals() {
        // The first lambda matches a regular formal at index 1; the rest
        // match the varargs formal at index 2.
        mixedRunner("hi", s -> {}, t -> {}, u -> {});
    }

    // ----------------------------------------------------------------------
    // @NonLeaked on a varargs functional-interface formal.  Each varargs
    // actual lambda should consult that annotation; previously this code
    // path crashed on the 2nd+ lambda before getEffectiveAnnotation was
    // even reached.
    @SafeVarargs
    static void nonLeakedVarargs(@NonLeaked Consumer<String>... consumers) {
        for (Consumer<String> c : consumers) {
            c.accept("");
        }
    }

    void nonLeakedAnnotation() {
        nonLeakedVarargs(s -> {}, t -> {});
    }

    // ----------------------------------------------------------------------
    // Function<T, R>... — non-Consumer functional interface to make sure
    // the fix isn't accidentally Consumer-specific.
    @SafeVarargs
    static <T> void applyAll(T input, Function<T, T>... fs) {
        T cur = input;
        for (Function<T, T> f : fs) {
            cur = f.apply(cur);
        }
    }

    void functionVarargs() {
        applyAll("seed", s -> s + "a", s -> s + "b", s -> s + "c");
    }

    // ----------------------------------------------------------------------
    // Generic varargs method with lambdas.  Adds type inference into the
    // mix to catch any interaction with type-arg resolution.
    @SafeVarargs
    static <T> void forEach(T initial, Consumer<? super T>... actions) {
        for (Consumer<? super T> a : actions) {
            a.accept(initial);
        }
    }

    void genericForEach() {
        forEach("x", s -> {}, s -> {});
        forEach(42, n -> {}, n -> {}, n -> {});
    }
}
