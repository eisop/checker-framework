import org.checkerframework.checker.nullness.qual.Nullable;
import org.jmlspecs.annotation.NonNull;

@SuppressWarnings("unchecked") // ignore heap pollution
class Lib {
    // element type inferred, array non-null
    static <T> void none(T... o) {}

    // element type inferred, array non-null
    static <T> void decl(@NonNull T... o) {}

    // element type nullable, array non-null
    static <T> void type(@Nullable T... o) {}

    // element type nullable, array nullable
    static <T> void typenn(@Nullable T @Nullable ... o) {}
}
