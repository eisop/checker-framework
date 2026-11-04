import org.checkerframework.checker.nullness.qual.Nullable;

/** Type-checked by MissingPolyInitialized against a classpath that lacks PolyInitialized. */
public class Input {
    @Nullable Object f;

    Object g() {
        return f;
    }
}
