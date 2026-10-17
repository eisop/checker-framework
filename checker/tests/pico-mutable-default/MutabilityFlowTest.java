import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

public class MutabilityFlowTest {

    @PolyMutable Object test(@PolyMutable Object obj) {
        Object o = new @PolyMutable Object();
        return o;
    }

    public @Immutable BaseClass testFlow(@Readonly BaseClass s) {
        if (s.getClass() == ImmutableClass.class) {
            return s;
        }
        return new ImmutableClass();
    }

    public @Immutable BaseClass testFlowNotEqual(@Readonly BaseClass s) {
        if (s.getClass() != ImmutableClass.class) {
            return new ImmutableClass();
        }
        return s;
    }

    @ReceiverDependentMutable class BaseClass {
        @Assignable @ReceiverDependentMutable BaseClass b;

        @PolyMutable BaseClass testFlowInner(@PolyMutable BaseClass this) {
            BaseClass local = b;
            return b;
        }
    }

    @Immutable class ImmutableClass extends BaseClass {}
}
