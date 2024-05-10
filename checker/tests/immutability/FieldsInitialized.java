import org.checkerframework.checker.immutability.qual.Assignable;
import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@ReceiverDependantMutable
public class FieldsInitialized {
    final @Immutable Object f1;
    @Immutable Object f2;
    final @ReceiverDependantMutable Object f3;
    @ReceiverDependantMutable Object f4;

    @Mutable Object f5;
    @Readonly Object f6;
    @Assignable @Immutable Object f7;
    @Assignable @ReceiverDependantMutable Object f8;
    @Assignable @Mutable Object f9;
    @Assignable @Readonly Object f10;

    // :: error: (initialization.fields.uninitialized)
    @ReceiverDependantMutable FieldsInitialized() {
        f1 = new @Immutable Object();
        f2 = new @Immutable Object();
        f3 = new @ReceiverDependantMutable Object();
        f4 = new @ReceiverDependantMutable Object();
        f5 = new @Mutable Object();
        f6 = new @Immutable Object();
    }
}
