import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;

public class IncompatibleCast {
    void foo() {
        @Mutable Object p = new Object();
        // call mutating method on p //

        // If only reference doesn't leak, accept casting to @Immutable so that clients
        // can have an @Immutable object. This is to allow initializing objects and then
        // once finished, cast it to @Immutable and nobody has alias to it.
        // :: warning: (cast.unsafe)
        Object o = (@Immutable Object) p;
    }
}
