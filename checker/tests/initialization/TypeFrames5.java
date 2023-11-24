import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeFrames5 {
    public @Nullable Object f;

    public TypeFrames5(boolean dummy) {
        @UnderInitialization(TypeFrames5.class) TypeFrames5 a = this;
    }

    public TypeFrames5(int dummy) {
        // :: error: (assignment.type.incompatible)
        @Initialized TypeFrames5 a = this;
    }
}
