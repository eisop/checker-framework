import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class PICOFlowTest {

    @PolyMutable Object test(@PolyMutable Object obj) {
        Object o = new @PolyMutable Object();
        return o;
    }

    // TODO: Implement flow refinement for PICO
    public @Immutable BaseClass testFlow(@Readonly BaseClass s) {
        if (s.getClass() == ImmutableClass.class) {
            return s;
        }
        return new ImmutableClass();
    }

    @ReceiverDependentMutable class BaseClass {
        @ReceiverDependentMutable BaseClass b;

        @PolyMutable BaseClass testFlowInner(@PolyMutable BaseClass this) {
            BaseClass local = b;
            return b;
        }
    }

    @Immutable class ImmutableClass extends BaseClass {}
}
