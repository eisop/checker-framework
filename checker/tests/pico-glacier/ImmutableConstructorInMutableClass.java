import org.checkerframework.checker.pico.qual.Immutable;

public class ImmutableConstructorInMutableClass {
    // :: error: (type.invalid.annotations.on.use)
    @Immutable public ImmutableConstructorInMutableClass() {}

    public void aMethod() {}
}
