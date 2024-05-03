import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

class InitializationBlockProblem {
    @ReceiverDependantMutable Object o;

    {
        this.o = new @Mutable Object();
        // :: error: (assignment.type.incompatible)
        this.o = new @Immutable Object();
    }
}
