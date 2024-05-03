import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@Immutable
class RDMAllowedAsMethodReceiver {
    // :: error: (type.invalid.annotations.on.use) :: error: (method.receiver.incompatible)
    void foo(@ReceiverDependantMutable RDMAllowedAsMethodReceiver this) {}
}

@Mutable
class AnotherExample {
    // :: error: (type.invalid.annotations.on.use) :: error: (method.receiver.incompatible)
    void foo(@ReceiverDependantMutable AnotherExample this) {}
}
