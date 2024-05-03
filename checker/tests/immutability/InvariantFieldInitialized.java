import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@Immutable
public class InvariantFieldInitialized {
    @Immutable Object o1;
    @ReceiverDependantMutable Object o2;
    // Below two lines still need initialization, as otherwise there won't
    // be opportunity for them to be initialized after @Immutable object is
    // constructed. Yes, but they are still outside abstract state.
    @Mutable Object o3;
    @Readonly Object o4;

    @Immutable
    InvariantFieldInitialized() {
        o1 = new @Immutable Object();
        o2 = new @Immutable Object();
        o3 = new @Mutable Object();
        o4 = new @Mutable Object();
    }
}
