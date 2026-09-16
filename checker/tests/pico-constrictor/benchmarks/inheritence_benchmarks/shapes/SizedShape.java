import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;

// @immutable
@Immutable public abstract class SizedShape {
    // @viewmethod
    public int size(@Readonly SizedShape this) {
        return -1;
    }
}
