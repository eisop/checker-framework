import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

@ReceiverDependantMutable
public class FieldAssignment {

    @ReceiverDependantMutable Object f;

    void setFWithMutableReceiver(
            @UnderInitialization @Mutable FieldAssignment this, @Mutable Object o) {
        this.f = new @Mutable Object();
    }

    // TODO This is not specific to PICO type system. InitializationVisitor currently has this issue
    // of false positively
    // wanrning uninitialized fields when we use instance method to initialiaze fields
    public FieldAssignment() {
        // :: error: (method.invocation.invalid)
        setFWithMutableReceiver(new @Mutable Object());
    }

    void setFWithReceiverDependantMutableReceiver(
            @ReceiverDependantMutable FieldAssignment this, @ReceiverDependantMutable Object pimo) {
        // :: error: (illegal.field.write)
        this.f = pimo;
    }

    void setFWithImmutableReceiver(@Immutable FieldAssignment this, @Immutable Object imo) {
        // :: error: (illegal.field.write)
        this.f = imo;
    }
}
