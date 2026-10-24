// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2271
import viewpointtest.quals.*;

public class AnonymousDiamond {
    static class Box<T> {
        void set(T t) {}
    }

    // :: warning: (cast.unsafe.constructor.invocation)
    @A Box<@A Object> anonDiamond = new @A Box<>() {};
    // :: warning: (cast.unsafe.constructor.invocation)
    @A Box<@A Object> anonExplicit = new @A Box<@A Object>() {};
    // :: warning: (cast.unsafe.constructor.invocation)
    @A Box<@A Object> namedDiamond = new @A Box<>();

    @A Box<@A Object> overrides =
            // :: warning: (cast.unsafe.constructor.invocation)
            new @A Box<>() {
                @Override
                void set(@A Object o) {}
            };

    @A Box<@Top Object> overridesWrong =
            // :: warning: (cast.unsafe.constructor.invocation)
            new @A Box<>() {
                @Override
                // :: error: (override.param.invalid)
                void set(@A Object o) {}
            };
}
