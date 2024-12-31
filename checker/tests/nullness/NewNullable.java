import org.checkerframework.checker.nullness.qual.*;

public class NewNullable {
    Object o = new Object();
    Object nn = new @NonNull Object();
    // :: error: (nullness.on.new.object)
    @Nullable Object lazy = new @MonotonicNonNull Object();
    // :: error: (nullness.on.new.object)
    // :: error: (invalid.polymorphic.qualifier.use)
    @Nullable Object poly = new @PolyNull Object();
    // :: error: (nullness.on.new.object)
    @Nullable Object nbl = new @Nullable Object();
}
