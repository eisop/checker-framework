import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

@ReceiverDependentMutable public class ViewpointAdaptationRules {

    @Assignable @Readonly Object assignableReadonlyField;
    @ReceiverDependentMutable Object rdmField;
    @Immutable Object immutableField;
    @Assignable @Mutable Object assingableMuatbleField;

    @ReceiverDependentMutable ViewpointAdaptationRules(
            @Readonly Object readonlyObject,
            @ReceiverDependentMutable Object rdmObject,
            @Immutable Object immutableObject,
            @Mutable Object muatbleObject) {
        this.assignableReadonlyField = readonlyObject;
        this.rdmField = rdmObject;
        this.immutableField = immutableObject;
        this.assingableMuatbleField = muatbleObject;
    }

    void mutatableReceiver(
            @Mutable ViewpointAdaptationRules this,
            @Mutable Object mutableObject,
            @Immutable Object immutableObject,
            @Readonly Object readonlyObject) {
        this.assignableReadonlyField = mutableObject;
        this.rdmField = mutableObject;
        // :: error: (assignment.type.incompatible)
        this.immutableField = mutableObject;
        this.assingableMuatbleField = mutableObject;

        this.assignableReadonlyField = immutableObject;
        // :: error: (assignment.type.incompatible)
        this.rdmField = immutableObject; // The field is adapted to mutable
        this.immutableField = immutableObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = immutableObject;

        this.assignableReadonlyField = readonlyObject;
        // :: error: (assignment.type.incompatible)
        this.rdmField = readonlyObject;
        // :: error: (assignment.type.incompatible)
        this.immutableField = readonlyObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = readonlyObject;
    }

    void receiverDependentMutableReceiver(
            @ReceiverDependentMutable ViewpointAdaptationRules this,
            @Mutable Object mutableObject,
            @ReceiverDependentMutable Object rdmObject,
            @Immutable Object immutableObject,
            @Readonly Object readonlyObject) {

        this.assignableReadonlyField = mutableObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = mutableObject; // The field is still RDM and can not be assigned to mutable
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.immutableField = mutableObject;
        this.assingableMuatbleField = mutableObject;

        this.assignableReadonlyField = rdmObject;
        // :: error: (illegal.field.write)
        this.rdmField = rdmObject; // can not write to it as it is non assignable
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.immutableField = rdmObject; // can not write to it as it is non assignable
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = rdmObject;

        this.assignableReadonlyField = immutableObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = immutableObject;
        // :: error: (illegal.field.write)
        this.immutableField = immutableObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = immutableObject;

        this.assignableReadonlyField = readonlyObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = readonlyObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.immutableField = readonlyObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = readonlyObject;
    }

    void ImmutableReceiver(
            @Immutable ViewpointAdaptationRules this,
            @Mutable Object mutableObject,
            @Immutable Object immutableObject,
            @Readonly Object readonlyObject) {
        this.assignableReadonlyField = mutableObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = mutableObject; // The field is adapted to Immutable
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.immutableField = mutableObject;
        this.assingableMuatbleField = mutableObject;

        this.assignableReadonlyField = immutableObject;
        // :: error: (illegal.field.write)
        this.rdmField = immutableObject;
        // :: error: (illegal.field.write)
        this.immutableField = immutableObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = immutableObject;

        this.assignableReadonlyField = readonlyObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = readonlyObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.immutableField = readonlyObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = readonlyObject;
    }

    void ReadonlyReceiver(
            @Readonly ViewpointAdaptationRules this,
            @Mutable Object mutableObject,
            @Immutable Object immutableObject,
            @Readonly Object readonlyObject) {
        this.assignableReadonlyField = mutableObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = mutableObject; // Field is adpated to PICOLost
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.immutableField = mutableObject;
        this.assingableMuatbleField = mutableObject;

        this.assignableReadonlyField = immutableObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = immutableObject;
        // :: error: (illegal.field.write)
        this.immutableField = immutableObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = immutableObject;

        this.assignableReadonlyField = readonlyObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.rdmField = readonlyObject;
        // :: error: (assignment.type.incompatible) :: error: (illegal.field.write)
        this.immutableField = readonlyObject;
        // :: error: (assignment.type.incompatible)
        this.assingableMuatbleField = readonlyObject;
    }
}
