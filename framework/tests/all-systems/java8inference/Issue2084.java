// Test case for EISOP issue #2084:
// https://github.com/eisop/checker-framework/issues/2084
// Type argument inference of a generic method used to compute, and cache, the type of an
// implicitly typed lambda parameter before inferring the type argument that the parameter's type
// depends on, when the lambda body invokes a generic method on the parameter.  The parameter then
// had the uninferred type variable as its type, and the invocation on it crashed with
// "AsSuperVisitor: type is not an erased subtype of supertype".

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class Issue2084 {

    static <A, R> R of(A a, Function<A, R> f) {
        throw new Error();
    }

    static <A, R> R lambdaFirst(Function<A, R> f, A a) {
        throw new Error();
    }

    static <A, B, R> R of2(A a, B b, BiFunction<A, B, R> f) {
        throw new Error();
    }

    static <A> void each(A a, Consumer<A> f) {}

    static <A, R> R curried(A a, Function<A, Function<A, R>> f) {
        throw new Error();
    }

    static <A, B> B cycle(A a, Function<A, B> f, Function<B, A> g) {
        throw new Error();
    }

    interface ThrowingFunction<A, R, E extends Exception> {
        R apply(A a) throws E;
    }

    static <A, R, E extends Exception> R ofThrowing(A a, ThrowingFunction<A, R, E> f) throws E {
        throw new Error();
    }

    interface Mappable<T> {
        <S> Mappable<S> map(Function<? super T, ? extends S> f);
    }

    static class Holder {
        final List<String> list;
        final String[] array;

        Holder(List<String> list, String[] array) {
            this.list = list;
            this.array = array;
        }
    }

    static class GenericConstructor {
        <A, R> GenericConstructor(A a, Function<A, R> f) {}
    }

    String[] original(List<String> list) {
        return of(list, l -> l.toArray(new String[0]));
    }

    String[] lambdaFirst(List<String> list) {
        return lambdaFirst(l -> l.toArray(new String[0]), list);
    }

    String[] biFunction(List<String> list, List<Integer> ints) {
        return of2(ints, list, (i, l) -> l.toArray(new String[0]));
    }

    void consumer(List<String> list) {
        each(list, l -> l.toArray(new String[0]));
    }

    GenericConstructor genericConstructor(List<String> list) {
        return new GenericConstructor(list, l -> l.toArray(new String[0]));
    }

    String[] nested(List<String> list) {
        return of(list, l -> of(l, m -> m.toArray(new String[0])));
    }

    String[] returnedLambda(List<String> list) {
        return curried(list, l -> m -> m.toArray(new String[0]));
    }

    String[] blockBody(List<String> list, boolean b) {
        return of(
                list,
                l -> {
                    if (b) {
                        return l.toArray(new String[0]);
                    }
                    return b ? l.toArray(new String[0]) : l.toArray(new String[0]);
                });
    }

    String[] cycle(List<String> list) {
        return cycle(list, l -> l.toArray(new String[0]), s -> list);
    }

    String[] fieldAccessReceiver(Holder holder) {
        return of(holder, h -> h.list.toArray(new String[0]));
    }

    String[] genericCallReceiver(Holder holder) {
        return of(holder, h -> Arrays.asList(h.array).toArray(new String[0]));
    }

    Mappable<Integer> methodReferenceArgument(Mappable<String> mappable) {
        return of(mappable, m -> m.map(String::length));
    }

    Object[] conditional(List<String> list, boolean b) {
        return of(list, l -> b ? (b ? l.toArray(new String[0]) : l.toArray()) : l.toArray());
    }

    String[] throwingFunction(List<String> list) {
        return ofThrowing(list, l -> l.toArray(new String[0]));
    }
}
