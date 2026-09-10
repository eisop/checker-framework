import org.checkerframework.checker.nullness.qual.Nullable;

public class AnonymousClassTypeAnnotation {
    void test() {
        // :: error: (nullness.on.new.object)
        new @Nullable Object() {};
    }
}
