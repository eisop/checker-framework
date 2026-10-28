// Test cases for eisop#2336 and eisop#2337: type argument inference used to crash with
// "Cannot invoke AbstractType.capture(...) because the return value of AbstractType.asSuper(...)
// is null".

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class EisopIssue2336 {

    // eisop#2336: a lambda passed as `Supplier<? extends List<? extends T>>` to a `? extends`
    // target whose type argument is parameterized.  `List<? extends T>` was captured while reducing
    // `List<? extends T> <: List<? extends T>`, which gave T the lower bound `capture of ? extends
    // T`, whose supertypes are unknown.
    static <T> List<T> defer(Supplier<? extends List<? extends T>> s) {
        throw new Error();
    }

    void issue2336(List<List<String>> l) {
        List<? extends List<String>> x = defer(() -> l);
    }

    // Variants of eisop#2336 that did not crash.
    void issue2336Controls(List<List<String>> l, List<Integer> li) {
        List<? extends Integer> y = defer(() -> li);
        List<List<String>> z = defer(() -> l);
    }

    // eisop#2337: a generic call with a raw method reference or a lambda, inside a lambda passed to
    // a generic method.  The outer call's inference variables were instantiated before the lambda
    // constraint was reduced, so the inner call was not reduced as part of the outer problem, but
    // its additional argument constraints were, with its inference variables unconstrained.
    interface Box<X> {
        X get();
    }

    static <T, R> List<R> each(List<T> ts, Function<? super T, ? extends R> f) {
        throw new Error();
    }

    static <T, R> List<R> eachInvariant(List<T> ts, Function<T, R> f) {
        throw new Error();
    }

    List<List<String>> issue2337MethodRef(List<List<Box<String>>> in) {
        return each(in, l -> each(l, Box::get));
    }

    List<List<String>> issue2337Lambda(List<List<Box<String>>> in) {
        return each(in, l -> each(l, b -> b.get()));
    }

    List<List<String>> issue2337Invariant(List<List<Box<String>>> in) {
        return eachInvariant(in, l -> eachInvariant(l, Box::get));
    }

    // The inner call is inferred with the real type of the lambda parameter `l`, so the type
    // argument of the element type of the result is the one of `Box`.
    List<List<@NonNull String>> issue2337Nullable(List<List<Box<@Nullable String>>> in) {
        // :: error: (type.arguments.not.inferred)
        return each(in, l -> each(l, Box::get));
    }

    List<List<@NonNull String>> issue2337NullableLambda(List<List<Box<@Nullable String>>> in) {
        // :: error: (return.type.incompatible)
        return each(in, l -> each(l, b -> b.get()));
    }

    // The case without a target type, which needs `var`, is in EisopIssue2337NoTarget.java.

    // Variants of eisop#2337 that did not crash.
    List<String> issue2337NotNested(List<List<Box<String>>> in) {
        return each(in.get(0), Box::get);
    }

    List<List<String>> issue2337Stream(List<List<Box<String>>> in) {
        return in.stream().map(l -> each(l, Box::get)).collect(Collectors.toList());
    }

    // The same cause as eisop#2337, with a lambda whose target type is proper from the start, and
    // with a nested call that is a direct argument with a proper formal parameter type.
    static <Q> Q g(Q q, Function<List<Box<String>>, List<String>> f) {
        throw new Error();
    }

    static <Q> Q h(Q q, List<String> l) {
        throw new Error();
    }

    String properLambdaTarget(List<List<Box<String>>> in) {
        return g("", l -> each(l, Box::get));
    }

    String properLambdaTargetLambda(List<List<Box<String>>> in) {
        return g("", l -> each(l, b -> b.get()));
    }

    String properFormalParameter(List<Box<String>> in) {
        return h("", each(in, Box::get));
    }
}
