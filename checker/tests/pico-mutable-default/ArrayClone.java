import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

public class ArrayClone {

    void cloneFromEveryViewpoint(
            Object @Mutable [] mutable,
            Object @Immutable [] immutable,
            Object @Readonly [] readonly,
            Object @ReceiverDependentMutable [] receiverDependent) {
        Object @Mutable [] mutableClone = mutable.clone();
        Object @Immutable [] immutableClone = immutable.clone();
        Object @Readonly [] readonlyClone = readonly.clone();
        Object @ReceiverDependentMutable [] receiverDependentClone = receiverDependent.clone();
    }
}
