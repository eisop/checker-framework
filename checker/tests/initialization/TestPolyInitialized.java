import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.PolyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class TestPolyInitialized {

    @NotOnlyInitialized String testStr;

    String test = "test";

    TestPolyInitialized(@UnknownInitialization String str) {
        this.testStr = str;
    }

    @PolyInitialized String identity(@PolyInitialized String str) {
        return str;
    }

    void test1() {
        identity(testStr);
    }

    void test2() {
        identity(test);
    }

    @Initialized String test3(@UnknownInitialization String str) {
        // :: error: (return.type.incompatible)
        return identity(str);
    }

    @UnknownInitialization String test4(@Initialized String str) {
        return identity(str);
    }
}
