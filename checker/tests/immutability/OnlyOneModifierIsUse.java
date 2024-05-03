import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Readonly;

public class OnlyOneModifierIsUse {

    // :: error: (type.invalid.conflicting.annos)
    // :: error: (initialization.field.uninitialized)
    @Readonly @Immutable Object field;
    // :: error: (type.invalid.conflicting.annos)
    // :: error: (initialization.field.uninitialized)
    String @Readonly @Immutable [] array;
}
