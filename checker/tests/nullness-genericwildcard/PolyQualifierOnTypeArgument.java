import org.checkerframework.checker.nullness.qual.PolyNull;

public class PolyQualifierOnTypeArgument<T> {
    void toArray(PolyQualifierOnTypeArgument<@PolyNull T> this) {}

    void test(PolyQualifierOnTypeArgument<?> p) {
        // Can not invoke this method because @PolyNull applies to both at the definition side and
        // get subsititue by the upper bound of unbounded wildcard.
        // found   : @NonNull PolyQualifierOnTypeArgument<?[ extends @Nullable Object super @NonNull
        // Void]>
        // required: @NonNull PolyQualifierOnTypeArgument<capture#01[ extends @Nullable Object super
        // @Nullable Void]>
        p.toArray();
    }
}
