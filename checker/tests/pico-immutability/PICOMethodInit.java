import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class PICOMethodInit {
    Object a;
    Object b;
    Object c;

    // TODO: Add postcondition qualifier for PICO, see EnsuresNonNull.java
    // :: error: (initialization.fields.uninitialized)
    PICOMethodInit() {
        initA();
        initB();
        initC();
    }

    void initA(@UnderInitialization(Object.class) PICOMethodInit this) {
        this.a = new Object();
    }

    void initB(@UnderInitialization(Object.class) PICOMethodInit this) {
        this.b = new Object();
    }

    void initC(@UnderInitialization(Object.class) PICOMethodInit this) {
        this.c = new Object();
    }
}
