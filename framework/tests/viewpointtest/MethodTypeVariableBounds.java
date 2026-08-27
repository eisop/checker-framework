import viewpointtest.quals.*;

public class MethodTypeVariableBounds {
    static class Methods {
        <T extends @ReceiverDependentQual Object> void noArg() {}

        <T extends @ReceiverDependentQual Object> void withArg(T t) {}

        // The @ReceiverDependentQual annotation on T is its explicit lower bound. The upper bound
        // is the implicit Object bound.
        <@ReceiverDependentQual T> void lowerNoArg() {}

        <@ReceiverDependentQual T> void lowerWithArg(T t) {}

        <@ReceiverDependentQual T extends @ReceiverDependentQual Object>
                void lowerAndUpperNoArg() {}

        <@ReceiverDependentQual T extends @ReceiverDependentQual Object> void lowerAndUpperWithArg(
                T t) {}
    }

    void topReceiver(
            @Top Methods methods,
            @Top Object top,
            @A Object a,
            @B Object b,
            @Bottom Object bottom) {
        // @Top viewpoint-adapts @ReceiverDependentQual to @Lost, so only @Bottom is within the
        // adapted method type parameter bound.
        // :: error: (viewpointtest.lost.in.bounds)
        methods.noArg();

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@Top Object>withArg(top);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@A Object>withArg(a);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@B Object>withArg(b);

        // :: error: (viewpointtest.lost.in.bounds)
        methods.<@Bottom Object>withArg(bottom);

        // :: error: (type.arguments.not.inferred) :: error: (viewpointtest.lost.in.bounds)
        methods.withArg(top);

        // :: error: (type.arguments.not.inferred) :: error: (viewpointtest.lost.in.bounds)
        methods.withArg(a);

        // :: error: (type.arguments.not.inferred) :: error: (viewpointtest.lost.in.bounds)
        methods.withArg(b);

        // :: error: (viewpointtest.lost.in.bounds)
        methods.withArg(bottom);

        // The lower bound @ReceiverDependentQual viewpoint-adapts to @Lost. Explicit type
        // arguments must be supertypes of that lower bound, so only @Top is valid.
        // :: error: (viewpointtest.lost.in.bounds)
        methods.lowerNoArg();
        // :: error: (viewpointtest.lost.in.bounds)
        methods.<@Top Object>lowerWithArg(top);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@A Object>lowerWithArg(a);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@B Object>lowerWithArg(b);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@Bottom Object>lowerWithArg(bottom);

        // Inference can choose @Top, which is above both the adapted lower bound and the argument.
        // :: error: (viewpointtest.lost.in.bounds)
        methods.lowerWithArg(top);
        // :: error: (viewpointtest.lost.in.bounds)
        methods.lowerWithArg(a);
        // :: error: (viewpointtest.lost.in.bounds)
        methods.lowerWithArg(b);

        // :: error: (viewpointtest.lost.parameter) :: error: (viewpointtest.lost.in.bounds)
        methods.lowerWithArg(bottom);

        // :: error: (viewpointtest.lost.in.bounds)
        methods.lowerAndUpperNoArg();

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@Top Object>lowerAndUpperWithArg(top);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@A Object>lowerAndUpperWithArg(a);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@B Object>lowerAndUpperWithArg(b);

        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        methods.<@Bottom Object>lowerAndUpperWithArg(bottom);

        // :: error: (type.arguments.not.inferred) :: error: (viewpointtest.lost.in.bounds)
        methods.lowerAndUpperWithArg(top);

        // :: error: (type.arguments.not.inferred) :: error: (viewpointtest.lost.in.bounds)
        methods.lowerAndUpperWithArg(a);

        // :: error: (type.arguments.not.inferred) :: error: (viewpointtest.lost.in.bounds)
        methods.lowerAndUpperWithArg(b);

        // :: error: (viewpointtest.lost.parameter) :: error: (viewpointtest.lost.in.bounds)
        methods.lowerAndUpperWithArg(bottom);
    }

    void aReceiver(
            @A Methods methods, @Top Object top, @A Object a, @B Object b, @Bottom Object bottom) {
        // @A viewpoint-adapts @ReceiverDependentQual to @A, so @A and @Bottom are within the
        // adapted method type parameter bound. Inference instantiates T to the adapted upper
        // bound @A, which is a valid type argument.
        methods.noArg();

        // :: error: (type.argument.type.incompatible)
        methods.<@Top Object>withArg(top);

        methods.<@A Object>withArg(a);

        // :: error: (type.argument.type.incompatible)
        methods.<@B Object>withArg(b);

        methods.<@Bottom Object>withArg(bottom);

        // :: error: (type.arguments.not.inferred)
        methods.withArg(top);

        // Inference succeeds: argument @A is within the adapted bound @A.
        methods.withArg(a);

        // :: error: (type.arguments.not.inferred)
        methods.withArg(b);

        methods.withArg(bottom);

        // The lower bound @ReceiverDependentQual viewpoint-adapts to @A. Explicit type arguments
        // must be supertypes of @A, so @Top and @A are valid.
        methods.lowerNoArg();
        methods.<@Top Object>lowerWithArg(top);
        methods.<@A Object>lowerWithArg(a);

        // :: error: (type.argument.type.incompatible)
        methods.<@B Object>lowerWithArg(b);

        // :: error: (type.argument.type.incompatible)
        methods.<@Bottom Object>lowerWithArg(bottom);

        // Inference chooses a type argument that is above both @A and the invocation argument.
        methods.lowerWithArg(top);
        methods.lowerWithArg(a);
        methods.lowerWithArg(b);
        methods.lowerWithArg(bottom);

        // Both bounds viewpoint-adapt to @A, so an explicit type argument must be exactly @A.
        methods.lowerAndUpperNoArg();

        // :: error: (type.argument.type.incompatible)
        methods.<@Top Object>lowerAndUpperWithArg(top);

        methods.<@A Object>lowerAndUpperWithArg(a);

        // :: error: (type.argument.type.incompatible)
        methods.<@B Object>lowerAndUpperWithArg(b);

        // :: error: (type.argument.type.incompatible)
        methods.<@Bottom Object>lowerAndUpperWithArg(bottom);

        // :: error: (type.arguments.not.inferred)
        methods.lowerAndUpperWithArg(top);

        // Inference chooses T = @A.
        methods.lowerAndUpperWithArg(a);

        // :: error: (type.arguments.not.inferred)
        methods.lowerAndUpperWithArg(b);

        // Inference chooses T = @A, which accepts the @Bottom argument.
        methods.lowerAndUpperWithArg(bottom);
    }
}
