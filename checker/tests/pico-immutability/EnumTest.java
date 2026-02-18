import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class EnumTest {
    enum Kind {
        SOME; // Enum constant is also @Immutable
    }

    // Shouldn't get warning. Implicitly applied @Immutable
    Kind defKind;
    // Enum is implicitly @Immutable, so using explicit @Immutable is allowed
    @Immutable Kind kind;
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentMutable Kind invalidKind;
    // :: error: (type.invalid.annotations.on.use)
    @Mutable Kind invalidKind2;
    // no error now
    @Readonly Kind invalidKind3;

    // :: error: (initialization.fields.uninitialized)
    EnumTest() {
        // Kind.SOME should be @Immutable
        kind = Kind.SOME;
    }

    void foo(/*immutable*/ MyEnum e) {
        // :: error: (type.invalid.annotations.on.use)
        @Mutable MyEnum mutableRef;
        @Immutable MyEnum immutableRef = e;

        @Mutable MutableEnum mutEnumMutRef = MutableEnum.M1;
        // :: error: (type.invalid.annotations.on.use)
        @Immutable MutableEnum mutEnumImmRef;
    }

    /*immutable*/
    private static enum MyEnum {
        T1,
        T2;
    }

    @Mutable // TODO Should we issue error here? Do we allow mutable enum?
    private static enum MutableEnum {
        M1,
        M2;
    }
}
