import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;

// Enum is now only immutable by default, not implicit
public class EnumTests {
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

    @Mutable
    private static enum MutableEnum {
        M1,
        M2;
    }
}
