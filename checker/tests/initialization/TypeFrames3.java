import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public class TypeFrames3 {
    public Object f;

    public TypeFrames3(boolean dummy) {
        initF();
        foo();
    }

    public TypeFrames3(int dummy) {
        // :: error: (method.invocation.invalid)
        foo();
        f = new Object();
    }

    @EnsuresNonNull("this.f")
    public void initF(@UnknownInitialization TypeFrames3 this) {
        f = new Object();
    }

    public void foo(@UnknownInitialization(TypeFrames3.class) TypeFrames3 this) {}
}
