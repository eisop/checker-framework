import viewpointtest.quals.*;

public class MethodTypeVariableBounds {
    static class Methods {
        <T extends @ReceiverDependentQual Object> void noArg() {}

        <T extends @ReceiverDependentQual Object> void withArg(T t) {}
    }

    void topReceiver(
            @Top Methods methods,
            @Top Object top,
            @A Object a,
            @B Object b,
            @Bottom Object bottom) {
        // @Top viewpoint-adapts @ReceiverDependentQual to @Lost, so only @Bottom is within the
        // adapted method type parameter bound.
        // :: error: (type.argument.type.incompatible)
        methods.noArg();

        // :: error: (type.argument.type.incompatible)
        methods.<@Top Object>withArg(top);

        // :: error: (type.argument.type.incompatible)
        methods.<@A Object>withArg(a);

        // :: error: (type.argument.type.incompatible)
        methods.<@B Object>withArg(b);

        methods.<@Bottom Object>withArg(bottom);

        // :: error: (type.arguments.not.inferred)
        methods.withArg(top);

        // :: error: (type.arguments.not.inferred)
        methods.withArg(a);

        // :: error: (type.arguments.not.inferred)
        methods.withArg(b);

        methods.withArg(bottom);
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
    }
}
