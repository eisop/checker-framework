package nobytecodestoragelib;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class Lib {
    public static @Nullable Object nullable() {
        return null;
    }

    /** The return type is defaulted to {@code @NonNull}; no annotation is written in the source. */
    public static Object nonNull() {
        return "";
    }
}
