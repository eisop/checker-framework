import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;

@Immutable
public class MethodReceiverNotInhericClassBound {

    // :: error: (method.receiver.incompatible)  :: error: (type.invalid.annotations.on.use)
    void bar(@Mutable MethodReceiverNotInhericClassBound this) {}
}
