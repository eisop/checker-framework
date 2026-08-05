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

    static class LowerBoundC {
        // The @ReceiverDependentQual annotation on T is its explicit lower bound. The upper bound
        // is the implicit Object bound.
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        <@ReceiverDependentQual T> LowerBoundC() {}

        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        <@ReceiverDependentQual T> LowerBoundC(T t) {}
    }

    static class LowerAndUpperBoundC {
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        <@ReceiverDependentQual T extends @ReceiverDependentQual Object> LowerAndUpperBoundC() {}

        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        <@ReceiverDependentQual T extends @ReceiverDependentQual Object> LowerAndUpperBoundC(T t) {}
    }

    void topViewpoint(@Top Object top, @A Object a, @B Object b, @Bottom Object bottom) {
        // Constructed type @Top adapts @ReceiverDependentQual to @Lost. Creating @Top is also
        // forbidden by the viewpoint test checker.
        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new @Top C();

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@Top Object>@Top C(top);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@A Object>@Top C(a);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@B Object>@Top C(b);

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new <@Bottom Object>@Top C(bottom);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top C(top);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top C(a);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top C(b);

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new @Top C(bottom);

        // The lower bound @ReceiverDependentQual viewpoint-adapts to @Lost. Explicit type
        // arguments must be supertypes of that lower bound, so only @Top is valid.
        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerBoundC();

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new <@Top Object>@Top LowerBoundC(top);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@A Object>@Top LowerBoundC(a);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@B Object>@Top LowerBoundC(b);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@Bottom Object>@Top LowerBoundC(bottom);

        // Inference can choose @Top, which is above both the adapted lower bound and the argument.
        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerBoundC(top);

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerBoundC(a);

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerBoundC(b);

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.parameter)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerBoundC(bottom);

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerAndUpperBoundC();

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@Top Object>@Top LowerAndUpperBoundC(top);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@A Object>@Top LowerAndUpperBoundC(a);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@B Object>@Top LowerAndUpperBoundC(b);

        // :: error: (new.class.type.invalid) :: error: (type.argument.type.incompatible)
        // :: error: (viewpointtest.lost.in.bounds)
        new <@Bottom Object>@Top LowerAndUpperBoundC(bottom);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerAndUpperBoundC(top);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerAndUpperBoundC(a);

        // :: error: (new.class.type.invalid) :: error: (type.arguments.not.inferred)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerAndUpperBoundC(b);

        // :: error: (new.class.type.invalid) :: error: (viewpointtest.lost.parameter)
        // :: error: (viewpointtest.lost.in.bounds)
        new @Top LowerAndUpperBoundC(bottom);
    }

    @SuppressWarnings("cast.unsafe.constructor.invocation")
    void aViewpoint(@Top Object top, @A Object a, @B Object b, @Bottom Object bottom) {
        // Constructed type @A adapts @ReceiverDependentQual to @A, so @A and @Bottom are within
        // the adapted constructor type parameter bound. Inference instantiates T to @A for the
        // no-arg constructor.
        new @A C();

        // :: error: (type.argument.type.incompatible)
        new <@Top Object>@A C(top);

        new <@A Object>@A C(a);

        // :: error: (type.argument.type.incompatible)
        new <@B Object>@A C(b);

        new <@Bottom Object>@A C(bottom);

        // :: error: (type.arguments.not.inferred)
        new @A C(top);

        // Inference succeeds: argument @A is within the adapted bound @A.
        new @A C(a);

        // :: error: (type.arguments.not.inferred)
        new @A C(b);

        new @A C(bottom);

        // The lower bound @ReceiverDependentQual viewpoint-adapts to @A. Explicit type arguments
        // must be supertypes of @A, so @Top and @A are valid.
        new @A LowerBoundC();

        new <@Top Object>@A LowerBoundC(top);

        new <@A Object>@A LowerBoundC(a);

        // :: error: (type.argument.type.incompatible)
        new <@B Object>@A LowerBoundC(b);

        // :: error: (type.argument.type.incompatible)
        new <@Bottom Object>@A LowerBoundC(bottom);

        // Inference chooses a type argument that is above both @A and the invocation argument.
        new @A LowerBoundC(top);

        new @A LowerBoundC(a);

        new @A LowerBoundC(b);

        new @A LowerBoundC(bottom);

        // Both bounds viewpoint-adapt to @A, so an explicit type argument must be exactly @A.
        new @A LowerAndUpperBoundC();

        // :: error: (type.argument.type.incompatible)
        new <@Top Object>@A LowerAndUpperBoundC(top);

        new <@A Object>@A LowerAndUpperBoundC(a);

        // :: error: (type.argument.type.incompatible)
        new <@B Object>@A LowerAndUpperBoundC(b);

        // :: error: (type.argument.type.incompatible)
        new <@Bottom Object>@A LowerAndUpperBoundC(bottom);

        // :: error: (type.arguments.not.inferred)
        new @A LowerAndUpperBoundC(top);

        // Inference chooses T = @A.
        new @A LowerAndUpperBoundC(a);

        // :: error: (type.arguments.not.inferred)
        new @A LowerAndUpperBoundC(b);

        // Inference chooses T = @A, which accepts the @Bottom argument.
        new @A LowerAndUpperBoundC(bottom);
    }
}
