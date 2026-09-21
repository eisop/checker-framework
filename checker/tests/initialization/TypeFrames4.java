import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public class TypeFrames4 {
    public Object f;

    public TypeFrames4(boolean dummy) {
        initF();
        @UnderInitialization(TypeFrames4.class) TypeFrames4 a = this;
    }

    public TypeFrames4(int dummy) {
        // :: error: (assignment.type.incompatible)
        @UnderInitialization(TypeFrames4.class) TypeFrames4 a = this;
        f = new Object();
    }

    @EnsuresNonNull("this.f")
    public void initF(@UnknownInitialization TypeFrames4 this) {
        f = new Object();
    }
}
