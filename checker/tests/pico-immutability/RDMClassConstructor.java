import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public @ReceiverDependentMutable class RDMClassConstructor {

    @Readonly Object readonlyField;
    @ReceiverDependentMutable Object rdmField;
    @Immutable Object immutableField;

    // :: error: (initialization.fields.uninitialized)
    @ReceiverDependentMutable RDMClassConstructor(
            @Mutable Object mutableObject,
            @ReceiverDependentMutable Object rdmObject,
            @Immutable Object immutableObject) {}

    @ReceiverDependentMutable RDMClassConstructor(
            @ReceiverDependentMutable Object rdmObject, @Immutable Object immutableObject) {
        this.readonlyField = rdmObject;
        this.readonlyField = immutableObject;

        this.rdmField = rdmObject;
        // :: error: (assignment.type.incompatible)
        this.rdmField = immutableObject;

        // :: error: (assignment.type.incompatible)
        this.immutableField = rdmObject;
        this.immutableField = immutableObject;
    }

    void invokeConstructor(
            @Readonly Object readonlyObejct,
            @Mutable Object mutableObject,
            @ReceiverDependentMutable Object rdmObject,
            @Immutable Object immutableObject) {
        new @Mutable RDMClassConstructor(mutableObject, immutableObject);
        // :: error: (argument.type.incompatible)
        new @Mutable RDMClassConstructor(readonlyObejct, immutableObject);
        // :: error: (argument.type.incompatible)
        new @Mutable RDMClassConstructor(rdmObject, immutableObject);
        // :: error: (argument.type.incompatible)
        new @Mutable RDMClassConstructor(immutableObject, immutableObject);

        new @ReceiverDependentMutable RDMClassConstructor(rdmObject, immutableObject);
        // :: error: (argument.type.incompatible)
        new @ReceiverDependentMutable RDMClassConstructor(readonlyObejct, immutableObject);
        // :: error: (argument.type.incompatible)
        new @ReceiverDependentMutable RDMClassConstructor(mutableObject, immutableObject);
        // :: error: (argument.type.incompatible)
        new @ReceiverDependentMutable RDMClassConstructor(immutableObject, immutableObject);

        new @Immutable RDMClassConstructor(immutableObject, immutableObject);
        // :: error: (argument.type.incompatible)
        new @Immutable RDMClassConstructor(readonlyObejct, immutableObject);
        // :: error: (argument.type.incompatible)
        new @Immutable RDMClassConstructor(mutableObject, immutableObject);
        // :: error: (argument.type.incompatible)
        new @Immutable RDMClassConstructor(rdmObject, immutableObject);

        // :: error: (argument.type.incompatible) :: error: (constructor.invocation.invalid)
        new @Readonly RDMClassConstructor(readonlyObejct, immutableObject);
    }
}
