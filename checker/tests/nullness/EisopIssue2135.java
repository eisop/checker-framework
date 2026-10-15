// Test case for https://github.com/eisop/checker-framework/issues/2135 :
// type-argument inference must resolve a polymorphic qualifier on the return type of a method
// invocation that is nested in the inference problem of an enclosing invocation, just as it is
// resolved when the nested invocation is not part of an enclosing inference problem.
// See checker/tests/nonempty/EisopIssue2135.java for a polymorphic qualifier on the receiver.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class EisopIssue2135 {

    static <T> T id(T t) {
        return t;
    }

    static <R> R get(Supplier<R> s) {
        return s.get();
    }

    static <A, R> R apply(A a, Function<A, R> f) {
        return f.apply(a);
    }

    // The return type mentions U, so an invocation of wrap is a poly expression.
    static <U> @PolyNull List<U> wrap(@PolyNull Object o, U u) {
        throw new RuntimeException();
    }

    static <U> @PolyNull List<U> wrapList(@PolyNull Object o, List<U> l) {
        throw new RuntimeException();
    }

    List<Integer> notNested(Object o) {
        return wrap(o, 1);
    }

    List<Integer> nestedInArgument(Object o) {
        return id(wrap(o, 1));
    }

    List<Integer> nestedInLambda(Object o) {
        return get(() -> wrap(o, 1));
    }

    List<Integer> nestedInLambdaUsingParameter(Object o) {
        return apply(o, x -> wrap(x, 1));
    }

    // The second argument is a poly expression, but its formal parameter type contains no
    // polymorphic qualifier, so it does not affect the instantiation.
    List<Integer> polyExpressionArgument(Object o, List<Integer> list) {
        return id(wrapList(o, id(list)));
    }

    @Nullable List<Integer> nullableNestedInArgument(@Nullable Object o) {
        return id(wrap(o, 1));
    }

    @Nullable List<Integer> nullableNestedInLambda(@Nullable Object o) {
        return get(() -> wrap(o, 1));
    }

    // The polymorphic qualifier is instantiated to @Nullable, so each of the following is an error,
    // just as it is when the invocation of wrap is not nested.

    List<Integer> nullableNotNested(@Nullable Object o) {
        // :: error: (return.type.incompatible)
        return wrap(o, 1);
    }

    List<Integer> nullableToNonNullNestedInArgument(@Nullable Object o) {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return id(wrap(o, 1));
    }

    List<Integer> nullableToNonNullNestedInLambda(@Nullable Object o) {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return get(() -> wrap(o, 1));
    }

    List<Integer> nullableToNonNullNestedInLambdaUsingParameter(@Nullable Object o) {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return apply(o, x -> wrap(x, 1));
    }

    List<Integer> nullableToNonNullPolyExpressionArgument(@Nullable Object o, List<Integer> list) {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return id(wrapList(o, id(list)));
    }
}
