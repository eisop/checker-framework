import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

// Initialization check only applies to possible non-assignable field, i.e. field in @RDM and
// @Immutable class without explict @Assignable annotation. Javac will handle final field
// initialization.
public class PICOInitialization {

    @ReceiverDependentMutable class RDMClassInitialized {
        final @Immutable Object f1;
        @Immutable Object f2;
        final @ReceiverDependentMutable Object f3;
        @ReceiverDependentMutable Object f4;

        @Mutable Object f5;
        @Readonly Object f6;
        @Assignable @Immutable Object f7;
        @Assignable @ReceiverDependentMutable Object f8;
        @Assignable @Mutable Object f9;
        @Assignable @Readonly Object f10;

        @ReceiverDependentMutable RDMClassInitialized() {
            f1 = new @Immutable Object();
            f2 = new @Immutable Object();
            f3 = new @ReceiverDependentMutable Object();
            f4 = new @ReceiverDependentMutable Object();
            f5 = new @Mutable Object();
            f6 = new @Immutable Object();
        }
    }

    @ReceiverDependentMutable class RDMClassUninitalized {
        final @Immutable Object f1;
        @Immutable Object f2;
        final @ReceiverDependentMutable Object f3;
        @ReceiverDependentMutable Object f4;

        @Mutable Object f5;
        @Readonly Object f6;
        @Assignable @Immutable Object f7;
        @Assignable @ReceiverDependentMutable Object f8;
        @Assignable @Mutable Object f9;
        @Assignable @Readonly Object f10;

        // :: error: (initialization.fields.uninitialized)
        @ReceiverDependentMutable RDMClassUninitalized() {
            f1 = new @Immutable Object();
            f2 = new @Immutable Object();
            f3 = new @ReceiverDependentMutable Object();
        }
    }

    @Immutable class ImmutableClassInitialized {
        final @Immutable Object f1;
        @Immutable Object f2;
        final @ReceiverDependentMutable Object f3;
        @ReceiverDependentMutable Object f4;

        @Mutable Object f5;
        @Readonly Object f6;
        @Assignable @Immutable Object f7;
        @Assignable @ReceiverDependentMutable Object f8;
        @Assignable @Mutable Object f9;
        @Assignable @Readonly Object f10;

        @Immutable ImmutableClassInitialized() {
            f1 = new @Immutable Object();
            f2 = new @Immutable Object();
            f3 = new @Immutable Object();
            f4 = new @Immutable Object();
            f5 = new @Mutable Object();
            f6 = new @Immutable Object();
        }
    }

    @Immutable class ImmutableClassUninitalized {
        final @Immutable Object f1;
        @Immutable Object f2;
        final @ReceiverDependentMutable Object f3;
        @ReceiverDependentMutable Object f4;

        @Mutable Object f5;
        @Readonly Object f6;
        @Assignable @Immutable Object f7;
        @Assignable @ReceiverDependentMutable Object f8;
        @Assignable @Mutable Object f9;
        @Assignable @Readonly Object f10;

        // :: error: (initialization.fields.uninitialized)
        @Immutable ImmutableClassUninitalized() {
            f1 = new @Immutable Object();
            f2 = new @Immutable Object();
            f3 = new @Immutable Object();
        }
    }

    @Mutable class MutableClassAssignableNoCheck {
        final @Immutable Object f1;
        @Immutable Object f2;
        final @ReceiverDependentMutable Object f3;
        @ReceiverDependentMutable Object f4;

        @Mutable Object f5;
        @Readonly Object f6;
        @Assignable @Immutable Object f7;
        @Assignable @ReceiverDependentMutable Object f8;
        @Assignable @Mutable Object f9;
        @Assignable @Readonly Object f10;

        @Mutable MutableClassAssignableNoCheck() {
            f1 = new @Immutable Object();
            f3 = new @Mutable Object();
        }
    }
}
