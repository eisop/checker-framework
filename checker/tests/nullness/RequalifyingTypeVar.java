import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RequalifyingTypeVar {
    abstract static class Box<E extends @Nullable Object> {
        abstract E bare();

        abstract @NonNull E concreteNonNull();
    }

    void test(Box<@Nullable String> box) {
        @Nullable String nullable = box.bare();
        // :: error: (assignment.type.incompatible)
        @NonNull String badNonNull = box.bare();

        @NonNull String nonNull = box.concreteNonNull();
        @Nullable String alsoNullable = box.concreteNonNull();
    }
}
