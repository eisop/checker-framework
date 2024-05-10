import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@Immutable
public class ImmutableConstructor {

    @Readonly Object rof;
    @ReceiverDependantMutable Object pif;
    @Immutable Object imf;

    // :: error: (initialization.fields.uninitialized)
    @Immutable ImmutableConstructor(
            @Mutable Object mo, @ReceiverDependantMutable Object po, @Immutable Object io) {}

    // Even if the first argument is @ReceiverDependantMutable, aliased @Mutable object cannot be
    // captured by pif,
    // because @Immutable constructor return type only allows @Immutable object to be captured after
    // viewpoint adaptation. So it's still safe to have @ReceiverDependantMutable arguemnt in
    // immutable constructor
    @Immutable
    ImmutableConstructor(@ReceiverDependantMutable Object po, @Immutable Object io) {
        this.rof = po;
        this.rof = io;

        this.pif = io;
        // :: error: (assignment.type.incompatible)
        this.pif = po;

        this.imf = io;
        // :: error: (assignment.type.incompatible)
        this.imf = po;
    }

    void invokeConstructor(
            @Readonly ImmutableConstructor this,
            @Readonly Object ro,
            @Mutable Object mo,
            @ReceiverDependantMutable Object po,
            @Immutable Object io) {
        new @Immutable ImmutableConstructor(io, io);

        // :: error: (constructor.invocation.invalid)
        new @Mutable ImmutableConstructor(mo, io);

        // constructor.invocation.invalid propgates before annotation invalid messages and stops
        // :: error: (constructor.invocation.invalid)
        new @ReceiverDependantMutable ImmutableConstructor(po, io);

        // :: error: (constructor.invocation.invalid) :: error: (pico.new.invalid)
        new @Readonly ImmutableConstructor(ro, io);
    }
}
