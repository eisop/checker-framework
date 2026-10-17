import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;

@Immutable class InitializationFieldWrites {
    static @Readonly InitializationFieldWrites readonlyOther;

    int field;

    {
        field = 1;
        this.field = 2;

        // :: error: (illegal.field.write)
        readonlyOther.field = 3;
    }

    InitializationFieldWrites(
            @Readonly InitializationFieldWrites other, int @Readonly [] readonlyArray) {
        field = 4;
        this.field = 5;
        this.field += 1;
        this.field++;

        // :: error: (illegal.field.write)
        other.field = 6;
        // :: error: (illegal.field.write)
        other.field += 1;
        // :: error: (illegal.field.write)
        other.field++;
        // :: error: (illegal.array.write)
        readonlyArray[0] = 7;
    }

    void initialize(
            @UnderInitialization(InitializationFieldWrites.class) InitializationFieldWrites this,
            @Readonly InitializationFieldWrites other) {
        this.field = 8;

        // :: error: (illegal.field.write)
        other.field = 9;
    }
}
