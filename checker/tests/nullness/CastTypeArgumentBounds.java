// The type arguments of a cast's target type are checked against their bounds, as those of a
// variable's type are.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

class CastTypeArgumentBounds {
    static class Box<T extends @NonNull Object> {}

    static class Outer<T extends @NonNull Object> {
        class Inner<S extends @NonNull Object> {}
    }

    @SuppressWarnings("unchecked") // javac warns about the casts themselves
    void casts(Object o) {
        // :: error: (type.argument.type.incompatible)
        Object a = (Box<@Nullable String>) o;
        // :: error: (type.argument.type.incompatible)
        Object b = (@Nullable Box<@Nullable String>) o;
        // :: error: (type.argument.type.incompatible)
        Object c = (List<Box<@Nullable String>>) o;
        // :: error: (type.argument.type.incompatible)
        Object d = (Outer<@Nullable String>.Inner<String>) o;
        // :: error: (type.argument.type.incompatible)
        Object e = (Outer<String>.Inner<@Nullable String>) o;

        Object f = (Box<String>) o;
        Object g = (List<Box<String>>) o;
        Object h = (Outer<String>.Inner<String>) o;
    }

    // The same type arguments in a variable's type.
    void variables() {
        // :: error: (type.argument.type.incompatible)
        Box<@Nullable String> a = null;
        // :: error: (type.argument.type.incompatible)
        Outer<@Nullable String>.Inner<String> d = null;
    }
}
