import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

// @NullUnmarked undoes the enclosing @NullMarked's @NonNull upper-bound default. This is the
// defaulting half of the alias, so unlike NullUnmarkedScope it needs no -AonlyAnnotatedFor.
@NullMarked
public class NullUnmarkedUndoesDefault {

    static <T> void marked(T t) {}

    @NullUnmarked
    static <T> void unmarked(T t) {}

    static void caller(@Nullable String nble) {
        // T's bound is @NonNull here, from the class's @NullMarked.
        // :: error: (type.arguments.not.inferred)
        marked(nble);
        // @NullUnmarked restores the unmarked bound, so this is accepted.
        unmarked(nble);
    }
}
