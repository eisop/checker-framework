import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.PolyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class TestPolyInitialized {

    @NotOnlyInitialized String testStr;

    String test = "test";

    TestPolyInitialized(@UnknownInitialization String str) {
        this.testStr = identity(str);
        // :: error: (assignment.type.incompatible)
        this.test = identity(str);
    }

    @PolyInitialized String identity(@UnknownInitialization TestPolyInitialized this, @PolyInitialized String str) {
        return str;
    }

    void test1() {
        @UnknownInitialization String receiver = identity(testStr);
    }

    void test2() {
        @Initialized String receiver = identity(test);
    }

    @Initialized String test3(@UnknownInitialization String str) {
        @UnknownInitialization String localStr = str;
        // :: error: (return.type.incompatible)
        return identity(str);
    }

    @UnknownInitialization String test4(@Initialized String str) {
        return identity(str);
    }

    @UnknownInitialization(Object.class) String test5(@Initialized String str) {
        return identity(str);
    }

    @Initialized String test6(@UnknownInitialization(Object.class) String str) {
        // :: error: (return.type.incompatible)
        return identity(str);
    }
}
