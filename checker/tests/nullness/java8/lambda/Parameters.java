// The location of a lambda affects locating the annotation for the lambda.

import org.checkerframework.checker.nullness.qual.*;

interface NullConsumer {
    void method(@Nullable String s);
}

interface NNConsumer {
    void method(@NonNull String s);
}

class LambdaParam {

    NullConsumer fn1 =
            // :: error: (lambda.param.type.incompatible)
            (@NonNull String i) -> {};
    NullConsumer fn2 = (@Nullable String i) -> {};
    // :: error: (lambda.param.type.incompatible)
    NullConsumer fn3 = (String i) -> {};
    NNConsumer fn4 = (String i) -> {};
    NNConsumer fn5 = (@Nullable String i) -> {};
    NNConsumer fn6 = (@NonNull String i) -> {};

    {
        // :: error: (lambda.param.type.incompatible)
        NullConsumer fn1 = (@NonNull String i) -> {};
        NullConsumer fn2 = (@Nullable String i) -> {};
        // :: error: (lambda.param.type.incompatible)
        NullConsumer fn3 = (String i) -> {};
        NNConsumer fn4 = (String i) -> {};
        NNConsumer fn5 = (@Nullable String i) -> {};
        NNConsumer fn6 = (@NonNull String i) -> {};
    }

    static {
        // :: error: (lambda.param.type.incompatible)
        NullConsumer fn1 = (@NonNull String i) -> {};
        NullConsumer fn2 = (@Nullable String i) -> {};
        // :: error: (lambda.param.type.incompatible)
        NullConsumer fn3 = (String i) -> {};
        NNConsumer fn4 = (String i) -> {};
        NNConsumer fn5 = (@Nullable String i) -> {};
        NNConsumer fn6 = (@NonNull String i) -> {};
    }

    static void foo() {
        NullConsumer fn1 =
                // :: error: (lambda.param.type.incompatible)
                (@NonNull String i) -> {};
        NullConsumer fn2 = (@Nullable String i) -> {};
        // :: error: (lambda.param.type.incompatible)
        NullConsumer fn3 = (String i) -> {};
        NNConsumer fn4 = (String i) -> {};
        NNConsumer fn5 = (@Nullable String i) -> {};
        NNConsumer fn6 = (@NonNull String i) -> {};
    }

    void bar() {
        NullConsumer fn1 =
                // :: error: (lambda.param.type.incompatible)
                (@NonNull String i) -> {};
        NullConsumer fn2 = (@Nullable String i) -> {};
        // :: error: (lambda.param.type.incompatible)
        NullConsumer fn3 = (String i) -> {};
        NNConsumer fn4 = (String i) -> {};
        NNConsumer fn5 = (@Nullable String i) -> {};
        NNConsumer fn6 = (@NonNull String i) -> {};
    }
}
