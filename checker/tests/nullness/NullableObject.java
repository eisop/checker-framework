import org.checkerframework.checker.nullness.qual.Nullable;

public class NullableObject {

    void foo() {
        // :: error: (nullness.on.new.object)
        Object nbl = new @Nullable Object();
    }

    void bar() {
        Object nn = new Object();
    }
}
