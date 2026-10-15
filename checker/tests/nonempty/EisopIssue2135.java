// Test case for https://github.com/eisop/checker-framework/issues/2135 :
// type-argument inference must resolve a polymorphic qualifier on the return type of a method
// invocation that is nested in the inference problem of an enclosing invocation, just as it is
// resolved when the nested invocation is not part of an enclosing inference problem.  Here the
// polymorphic qualifier of Stream.map is instantiated by its receiver.
// See checker/tests/nullness/EisopIssue2135.java for a polymorphic qualifier on a parameter.

import org.checkerframework.checker.nonempty.qual.NonEmpty;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class EisopIssue2135 {

    static <T> T id(T t) {
        return t;
    }

    static <A, R> R apply(A a, Function<A, R> f) {
        return f.apply(a);
    }

    Stream<Integer> nestedInArgument(List<String> list) {
        return id(list.stream().map(String::length));
    }

    Stream<Integer> nestedInLambda(List<String> list) {
        return apply(list, l -> list.stream().map(String::length));
    }

    Stream<Integer> nestedInExplicitLambda(List<String> list) {
        return apply(list, (List<String> l) -> l.stream().map(String::length));
    }

    Stream<Integer> explicitTypeArguments(List<String> list) {
        return EisopIssue2135.<List<String>, Stream<Integer>>apply(
                list, l -> l.stream().map(String::length));
    }

    @NonEmpty
    Stream<Integer> nonEmptyNotNested(@NonEmpty List<String> list) {
        return list.stream().map(String::length);
    }

    @NonEmpty
    Stream<Integer> nonEmptyNestedInArgument(@NonEmpty List<String> list) {
        return id(list.stream().map(String::length));
    }

    @NonEmpty
    Stream<Integer> nonEmptyNestedInArgumentWithLambda(@NonEmpty List<String> list) {
        return id(list.stream().map(s -> s.length()));
    }

    @NonEmpty
    Stream<Integer> nonEmptyGenericReceiverNestedInArgument() {
        return id(Stream.of("a", "bc").map(String::length));
    }

    @NonEmpty
    Stream<Integer> nonEmptyNestedInLambda(@NonEmpty List<String> list) {
        return apply(1, i -> list.stream().map(String::length));
    }

    @NonEmpty
    Stream<Integer> nonEmptyNestedInLambdaUsingParameter(@NonEmpty List<String> list) {
        return apply(list, l -> l.stream().map(String::length));
    }

    // The receiver might be empty, so the polymorphic qualifier is instantiated to
    // @UnknownNonEmpty and each of the following is an error, just as it is when the invocation of
    // map is not nested.

    @NonEmpty
    Stream<Integer> unknownNotNested(List<String> list) {
        // :: error: (return.type.incompatible)
        return list.stream().map(String::length);
    }

    @NonEmpty
    Stream<Integer> unknownNestedInArgument(List<String> list) {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return id(list.stream().map(String::length));
    }

    @NonEmpty
    Stream<Integer> unknownNestedInLambda(List<String> list) {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return apply(1, i -> list.stream().map(String::length));
    }

    @NonEmpty
    Stream<Integer> unknownNestedInLambdaUsingParameter(List<String> list) {
        // :: error: (return.type.incompatible) :: error: (type.arguments.not.inferred)
        return apply(list, l -> l.stream().map(String::length));
    }
}
