package permissivedefaultslib;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Unchecked code: not {@code @AnnotatedFor("nullness")}. */
public class UncheckedLib {
    public static Object get() {
        return "";
    }

    public static void take(Object value) {}

    public static @Nullable Object getNullable() {
        return null;
    }

    public static @NonNull Object getNonNull() {
        return "";
    }

    public static void takeNullable(@Nullable Object value) {}

    public static void takeNonNull(@NonNull Object value) {}
}
