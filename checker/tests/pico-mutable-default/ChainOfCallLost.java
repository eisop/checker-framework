// @skip-test
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

@ReceiverDependentMutable public class ChainOfCallLost {
    ChainOfCallLost c;

    ChainOfCallLost test(@Readonly ChainOfCallLost this) {
        return c.identity();
    }

    @PolyMutable ChainOfCallLost identity(@PolyMutable ChainOfCallLost this) {
        return this;
    }
}
