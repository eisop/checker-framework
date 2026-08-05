// @skip-test
// Missing Feature: The PICO initialization checker does not yet support postcondition
// qualifiers (like @EnsuresAssigned or @EnsuresNonNull). Because of this, it cannot
// verify that helper methods (like initA) initialize fields, resulting in a false
// positive (initialization.fields.uninitialized) in the constructor.
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.mutability.qual.Immutable;

@Immutable public class MutabilityMethodInit {
    Object a;
    Object b;
    Object c;

    // :: error: (initialization.fields.uninitialized)
    MutabilityMethodInit() {
        initA();
        initB();
        initC();
    }

    void initA(@UnderInitialization(Object.class) MutabilityMethodInit this) {
        this.a = new Object();
    }

    void initB(@UnderInitialization(Object.class) MutabilityMethodInit this) {
        this.b = new Object();
    }

    void initC(@UnderInitialization(Object.class) MutabilityMethodInit this) {
        this.c = new Object();
    }
}
