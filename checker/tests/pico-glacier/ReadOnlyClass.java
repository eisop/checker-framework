import org.checkerframework.checker.pico.qual.*;

// Classes can't be annotated ReadOnly in their declarations; @Readonly is only for method
// parameters.
// ::error: (glacier.readonly.class)
@Readonly public class ReadOnlyClass {}

class ReadOnlyMethodClass {
    @Readonly ReadOnlyClass roc;

    int @Readonly [] readOnlyIntArray;

    // ::error: (type.invalid.annotations.on.use)
    void takeReadOnlyString(@Readonly String foo) {}

    void takeReadOnlyArray(String @Readonly [] foo) {
        // ::error: (glacier.assignment.array)
        foo[0] = "Hello, world!";
    }

    void takeImmutableArray(String @Immutable [] foo) {
        // ::error: (glacier.assignment.array)
        foo[0] = "Hello, world!";
    }
}
