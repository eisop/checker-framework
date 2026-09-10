// Test case for an upcast whose cast type and expression type declare a different number of type
// arguments.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

public class UpcastToFewerTypeArguments {

    static class NullableList extends ArrayList<@Nullable String> {}

    static class Pair<A extends @Nullable Object, B extends @Nullable Object> {}

    static class StringPair extends Pair<@Nullable String, @Nullable String> {}

    // The expression's type declares no type parameter of its own, but the type hierarchy resolves
    // its binding for the cast type's type parameter.
    Iterable<@Nullable String> upcastToIterable(NullableList list) {
        return (Iterable<@Nullable String>) list;
    }

    List<@Nullable String> upcastToList(NullableList list) {
        return (List<@Nullable String>) list;
    }

    Pair<@Nullable String, @Nullable String> upcastToPair(StringPair p) {
        return (Pair<@Nullable String, @Nullable String>) p;
    }

    // The type arguments are still checked.
    List<@NonNull String> upcastToWrongTypeArgument(NullableList list) {
        // :: warning: (cast.unsafe)
        return (List<@NonNull String>) list;
    }

    Pair<@NonNull String, @Nullable String> upcastToWrongTypeArguments(StringPair p) {
        // :: warning: (cast.unsafe)
        return (Pair<@NonNull String, @Nullable String>) p;
    }

    // A downcast to a type with more type arguments is still not statically verifiable.
    @SuppressWarnings("unchecked") // The cast is an unchecked cast.
    List<@Nullable String> downcastFromObject(Object o) {
        // :: warning: (cast.unsafe)
        return (List<@Nullable String>) o;
    }
}
