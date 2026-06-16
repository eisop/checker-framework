import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

@ReceiverDependentMutable public class ChainOfCallLost {
    ChainOfCallLost c;

    ChainOfCallLost test(@Readonly ChainOfCallLost this) {
        return c.identity();
    }

    @PolyMutable ChainOfCallLost identity(@PolyMutable ChainOfCallLost this) {
        return this;
    }
}
