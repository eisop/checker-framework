import org.checkerframework.checker.nullness.qual.PolyNull;

public class PolyQualifierOnTypeArgument<T> {
    void toArray(PolyQualifierOnTypeArgument<@PolyNull T> this) {
        // This method has @PolyNull as receiver's type argument annotation.
    }

    void test(PolyQualifierOnTypeArgument<?> p) {
        // When invoking method on receiver using an unbounded wildcard as type argument, the LUB of
        // annotations on the bounds is @Nullable. Therefore, the declared receiver type get
        // substituted as @Nullable T, which is not compatible with an unbounded wildcard.
        // found   : @NonNull PolyQualifierOnTypeArgument<? [ extends @Nullable Object super
        // @NonNull Nulltype ]>
        // required: @NonNull PolyQualifierOnTypeArgument<capture#01 [ extends @Nullable Object
        // super @Nullable Nulltype ]>
        p.toArray();
    }
}
