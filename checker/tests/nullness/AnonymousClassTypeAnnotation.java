import org.checkerframework.checker.nullness.qual.Nullable;

public class AnonymousClassTypeAnnotation {
    @SuppressWarnings("nullness.on.supertype")
    void test() {
        // :: error: (nullness.on.new.object)
        new @Nullable Object() {};
    }
}
