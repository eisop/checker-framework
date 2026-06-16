import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Readonly;

// Done

// @immutable
@Immutable public abstract class SizedShape {
    // @viewmethod
    public int size(@Readonly SizedShape this) {
        return -1;
    }
}
