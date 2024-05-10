import org.checkerframework.checker.immutability.qual.Readonly;

public class ReadonlyConstructor {

    // :: error: (constructor.return.invalid)
    @Readonly ReadonlyConstructor() {}
}
