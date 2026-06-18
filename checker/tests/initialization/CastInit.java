import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class CastInit {

    public CastInit() {
        @UnknownInitialization CastInit t1 = (@UnknownInitialization CastInit) this;
        // In a constructor, `this` is @UnderInitialization; @UnderInitialization and @Initialized
        // are incomparable in the initialization hierarchy.
        // :: error: (cast.incomparable)
        @Initialized CastInit t2 = (@Initialized CastInit) this;
    }
}
