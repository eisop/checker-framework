import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

@Immutable public class EnumTest {
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

    // Java enums can have mutable fields, so @Mutable on enum declarations is allowed.
    // The enum defaulter (MutabilityEnumDefaultAnnotator) only applies @Immutable when no
    // explicit mutability annotation is present, so @Mutable properly overrides the default.
    @Mutable
    private static enum MutableEnum {
        M1,
        M2;
    }

    // A realistic mutable enum: enum instances with mutable state.
    @Mutable
    private static enum Counter {
        INSTANCE;

        private int count = 0;

        void increment(@Mutable Counter this) {
            count++;
        }

        int getCount(@Readonly Counter this) {
            return count;
        }
    }
}
