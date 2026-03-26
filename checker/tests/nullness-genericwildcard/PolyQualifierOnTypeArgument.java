import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class PolyQualifierOnTypeArgument<T> {
    void toArray(PolyQualifierOnTypeArgument<@PolyNull T> this) {
        // This method has @PolyNull on receiver's type argument.
    }

    void test(PolyQualifierOnTypeArgument<?> p) {
        // When calling toArray on a receiver with an unbounded wildcard type argument, the LUB of
        // the annotations on the wildcard's bounds is @Nullable. As a result, the receiver type is
        // substituted as PolyQualifierOnTypeArgument<@Nullable T>, which is not compatible with an
        // unbounded wildcard type argument.
        // If the method receiver type argument subtyping check is enabled, the error is:
        // found   : @NonNull PolyQualifierOnTypeArgument<? [ extends @Nullable Object super
        // @NonNull Nulltype ]>
        // required: @NonNull PolyQualifierOnTypeArgument<capture#01 [ extends @Nullable Object
        // super @Nullable Nulltype ]>
        p.toArray();
    }

    void test2(PolyQualifierOnTypeArgument<T> p) {
        p.toArray(); // same bound mismatch as unbounded wildcard cass
    }

    void test3(PolyQualifierOnTypeArgument<@NonNull T> p) {
        p.toArray(); // ok, the LUB is @NonNull, both bounds are @NonNull is compatiable with
        // @PolyNull T after substitution
    }

    void test4(PolyQualifierOnTypeArgument<@Nullable Object> p) {
        p.toArray(); // ok, the LUB is @Nullable, both bounds are @Nullable is compatiable
        // with @PolyNull T after substitution
    }

    void test5(PolyQualifierOnTypeArgument<Object> p) {
        p.toArray(); // ok, the LUB is @NonNull, both bounds are @NonNull is compatiable with
        // @PolyNull T after substitution
    }
}
