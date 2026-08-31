// Type-argument inference must resolve a polymorphic qualifier on the compile-time declaration
// of a method reference, just as it does for the same call written as a lambda.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.stream.Stream;

public class PolyNullInference {

    interface Lib {}

    static @PolyNull Lib polyCast(@PolyNull Object o) {
        throw new RuntimeException();
    }

    @PolyNull Lib polyCastBound(@PolyNull Object o) {
        throw new RuntimeException();
    }

    void directCall(Object nonNull, @Nullable Object nble) {
        Lib x = polyCast(nonNull);
        @Nullable Lib y = polyCast(nble);
    }

    Stream<Lib> lambda(Stream<Object> s) {
        return s.map(o -> polyCast(o));
    }

    Stream<Lib> staticMethodRef(Stream<Object> s) {
        return s.map(PolyNullInference::polyCast);
    }

    Stream<Lib> boundMethodRef(Stream<Object> s) {
        return s.map(this::polyCastBound);
    }

    Stream<Lib> explicitTypeArgument(Stream<Object> s) {
        return s.<Lib>map(PolyNullInference::polyCast);
    }

    Stream<Lib> classCastMethodRef(Stream<Object> s) {
        return s.map(Lib.class::cast);
    }

    Stream<@Nullable Lib> nullableStaticMethodRef(Stream<@Nullable Object> s) {
        return s.map(PolyNullInference::polyCast);
    }

    // The polymorphic qualifier is instantiated to @Nullable, so the inferred type argument is
    // @Nullable Lib, which is not a subtype of @NonNull Lib.  The lambda below fails the same way.
    Stream<Lib> nullableToNonNullMethodRef(Stream<@Nullable Object> s) {
        // :: error: (type.arguments.not.inferred)
        return s.map(PolyNullInference::polyCast);
    }

    Stream<Lib> nullableToNonNullLambda(Stream<@Nullable Object> s) {
        // :: error: (type.arguments.not.inferred)
        return s.map(o -> polyCast(o));
    }
}
