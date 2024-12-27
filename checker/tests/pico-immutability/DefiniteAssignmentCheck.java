import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class DefiniteAssignmentCheck {
    class ImplicitImmutableClass {
        // :: error: (initialization.field.uninitialized)
        @Readonly Object readonlyField; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA & RDM*/ Object a; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @Immutable Object b; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @Mutable Object c; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @ReceiverDependentMutable Object d; // initialization should be checked
        @Assignable /*RDM*/ Object e;
        @Assignable @Immutable Object f;
        @Assignable @Mutable Object g;
        @Assignable @ReceiverDependentMutable Object h;
        @Assignable @Readonly Object readonlyField2;
    }

    @Immutable class ExplicitImmutableClass {
        // :: error: (initialization.field.uninitialized)
        @Readonly Object readonlyField; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA & RDM*/ Object a; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @Immutable Object b; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @Mutable Object c; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @ReceiverDependentMutable Object d; // initialization should be checked
        @Assignable /*RDM*/ Object e;
        @Assignable @Immutable Object f;
        @Assignable @Mutable Object g;
        @Assignable @ReceiverDependentMutable Object h;
        @Assignable @Readonly Object readonlyField2;
    }

    @ReceiverDependentMutable class RDMClass {
        // :: error: (initialization.field.uninitialized)
        @Readonly Object readonlyField; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA & RDM*/ Object a; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @Immutable Object b; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @Mutable Object c; // initialization should be checked
        // :: error: (initialization.field.uninitialized)
        /*RDA*/ @ReceiverDependentMutable Object d; // initialization should be checked
        @Assignable /*RDM*/ Object e;
        @Assignable @Immutable Object f;
        @Assignable @Mutable Object g;
        @Assignable @ReceiverDependentMutable Object h;
        @Assignable @Readonly Object readonlyField2;
    }

    @Mutable class MutableClass {
        @Readonly Object readonlyField;
        /*RDA & RDM*/ Object a;
        /*RDA*/ @Immutable Object b;
        /*RDA*/ @Mutable Object c;
        /*RDA*/ @ReceiverDependentMutable Object d;
        @Assignable /*RDM*/ Object e;
        @Assignable @Immutable Object f;
        @Assignable @Mutable Object g;
        @Assignable @ReceiverDependentMutable Object h;
        @Assignable @Readonly Object readonlyField2;
    }
}
