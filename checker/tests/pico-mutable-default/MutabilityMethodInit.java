// @skip-test
// TODO: implement ensuresAssigned for PICO
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.mutability.qual.Immutable;

@Immutable public class MutabilityMethodInit {
    Object a;
    Object b;
    Object c;

    // TODO: Add postcondition qualifier for PICO, see EnsuresNonNull.java
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
