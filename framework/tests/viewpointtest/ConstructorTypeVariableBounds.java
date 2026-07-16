import viewpointtest.quals.*;

public class ConstructorTypeVariableBounds {
    static class C {
        // No-arg generic constructor: type argument is unused, so inference instantiates T to
        // the adapted upper bound.
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        <T extends @ReceiverDependentQual Object> C() {}

        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        <T extends @ReceiverDependentQual Object> C(T t) {}
    }

    void topViewpoint(
            @Top Object top, @A Object a, @B Object b, @Bottom Object bottom) {
        // Constructed type @Top adapts @ReceiverDependentQual to @Lost. Creating @Top is also
        // forbidden by the viewpoint test checker.
        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        new @Top C();

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        new <@Top Object> @Top C(top);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        new <@A Object> @Top C(a);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        new <@B Object> @Top C(b);

        // :: error: (new.class.type.invalid)
        new <@Bottom Object> @Top C(bottom);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        new @Top C(top);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        new @Top C(a);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        new @Top C(b);

        // :: error: (new.class.type.invalid)
        new @Top C(bottom);
    }

    void aViewpoint(
            @Top Object top, @A Object a, @B Object b, @Bottom Object bottom) {
        // Constructed type @A adapts @ReceiverDependentQual to @A, so @A and @Bottom are within
        // the adapted constructor type parameter bound. Inference instantiates T to @A for the
        // no-arg constructor.
        // :: warning: (cast.unsafe.constructor.invocation)
        new @A C();

        // :: error: (type.argument.type.incompatible) :: warning: (cast.unsafe.constructor.invocation)
        new <@Top Object> @A C(top);

        // :: warning: (cast.unsafe.constructor.invocation)
        new <@A Object> @A C(a);

        // :: error: (type.argument.type.incompatible) :: warning: (cast.unsafe.constructor.invocation)
        new <@B Object> @A C(b);

        // :: warning: (cast.unsafe.constructor.invocation)
        new <@Bottom Object> @A C(bottom);

        // :: error: (type.arguments.not.inferred)
        new @A C(top);

        // Inference succeeds: argument @A is within the adapted bound @A.
        // :: warning: (cast.unsafe.constructor.invocation)
        new @A C(a);

        // :: error: (type.arguments.not.inferred)
        new @A C(b);

        // :: warning: (cast.unsafe.constructor.invocation)
        new @A C(bottom);
    }
}
