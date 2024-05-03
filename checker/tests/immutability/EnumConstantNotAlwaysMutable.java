import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

// It's equivalent to having @Immutable on every enum type
enum Kind {
    SOME; // Enum constant is also @Immutable
}

public class EnumConstantNotAlwaysMutable {

    // Shouldn't get warning. Implicitly applied @Immutable
    Kind defKind;
    // Enum is implicitly @Immutable, so using explicit @Immutable is allowed
    @Immutable Kind kind;
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependantMutable Kind invalidKind;
    // :: error: (type.invalid.annotations.on.use)
    @Mutable Kind invalidKind2;
    // no error now
    @Readonly Kind invalidKind3;

    // :: error: (initialization.fields.uninitialized)
    EnumConstantNotAlwaysMutable() {
        // Kind.SOME should be @Immutable
        kind = Kind.SOME;
    }
}
