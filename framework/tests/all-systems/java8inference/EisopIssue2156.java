// Test case for https://github.com/eisop/checker-framework/issues/2156
// An inexact method reference with a raw receiver type, such as Merged::name, passed to a generic
// method. The type argument of Merged was inferred as its bound, Object, instead of from the
// function type's parameter. That crashed the receiver check when the parameter's type argument
// was a type variable, and reported a false methodref.receiver.invalid otherwise.

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EisopIssue2156<A> {
    interface Merged<X> {
        String name();

        X get();
    }

    static <B, K> Predicate<Merged<B>> unique(Function<? super Merged<B>, K> keyExtractor) {
        throw new Error();
    }

    Predicate<Merged<A>> typeVariable = unique(Merged::name);

    Predicate<Merged<String>> declaredType = unique(Merged::name);

    // The return type of get mentions X, so X is inferred together with the call.
    Predicate<Merged<A>> returnMentionsX = unique(Merged::get);

    <A> Predicate<Merged<A>> shadowed() {
        return unique(Merged::name);
    }

    Stream<Merged<A>> nested(Stream<Merged<A>> s) {
        return s.filter(unique(Merged::name));
    }
}
