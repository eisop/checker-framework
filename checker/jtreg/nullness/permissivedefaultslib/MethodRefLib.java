package permissivedefaultslib;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/** Unchecked code: not {@code @AnnotatedFor("nullness")}. */
public class MethodRefLib {
    public static void unannotated(String value) {}

    public static void nonNull(@NonNull String value) {}

    public static void unannotatedElement(List<String> values) {}

    public static Object unannotatedReturn() {
        return "";
    }

    public static void varargs(String... values) {}

    /** A functional interface with an unannotated parameter type. */
    public interface Callback {
        void run(String value);
    }

    /** A functional interface with an unannotated return type. */
    public interface Namer {
        String name();
    }

    /** A functional interface with a parameter that is written {@code @Nullable}. */
    public interface NullableCallback {
        void run(@Nullable String value);
    }

    /** A functional interface with a type variable. */
    public interface Sink<T> {
        void accept(T value);
    }

    /** A class with an instance method, for an unbound method reference. */
    public static class Item {
        public void use() {}
    }

    /** A functional interface whose parameter is the receiver of an unbound method reference. */
    public interface ItemHandler {
        void handle(Item item);
    }
}
