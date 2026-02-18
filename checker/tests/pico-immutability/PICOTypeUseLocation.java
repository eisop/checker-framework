import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class PICOTypeUseLocation {
    // :: error: (type.invalid.conflicting.annos)
    @Readonly @Immutable Object field;
    // :: error: (type.invalid.conflicting.annos)
    String @Readonly @Immutable [] array;
    // In the abstract state
    int implicitImmutableInt;
    @Immutable int validInt;
    // If you want to exclude primitive(including boxed primitive) and String from
    // abstract state, use @Readonly to do this, but not @Mutable, because they can't
    // be mutated conceptually.
    // :: error: (type.invalid.annotations.on.use)
    @Readonly int implicitOverridenInt;
    // :: error: (type.invalid.annotations.on.use)
    @Mutable int invalidInt;
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentMutable int invalidInt2;

    // :: error: (initialization.fields.uninitialized)
    PICOTypeUseLocation() {}

    // :: error: (type.invalid.annotations.on.location) :: error: (invalid.polymorphic.qualifier)
    class PICOTypeUseLocationFail<@PolyMutable T, S extends @PolyMutable Object> {
        // :: error: (type.invalid.annotations.on.location) :: error: (constructor.return.invalid)
        @PolyMutable PICOTypeUseLocationFail() {}

        // :: error: (super.invocation.invalid) :: error: (constructor.return.invalid)
        @Readonly PICOTypeUseLocationFail(int a) {}
    }
}
