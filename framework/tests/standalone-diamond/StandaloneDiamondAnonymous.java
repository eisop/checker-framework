// @below-java9-jdk-skip-test

import org.checkerframework.framework.testchecker.util.Encrypted;
import org.checkerframework.framework.testchecker.util.PolyEncrypted;

// A diamond that creates an anonymous class is not inferred without its enclosing inference
// (GenericAnnotatedTypeFactory#getStandaloneTypeForNodeValue): the type arguments of its superclass
// are inferred from the super constructor.  See StandaloneDiamondNodeValue.java.
public class StandaloneDiamondAnonymous {

    static class Box<T> {
        // The constructor of Object, which this one invokes, is not polymorphic.
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        @PolyEncrypted Box(@PolyEncrypted T t) {}
    }

    static <U> void takes(@Encrypted Box<U> b) {}

    void anonymousClass(@Encrypted String e) {
        // The constructor of the anonymous class is not polymorphic.
        // :: error: (argument.type.incompatible)
        takes(new Box<>(e) {});
    }
}
