import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.PolyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class TestPolyInitialized {

    @NotOnlyInitialized String testStr;

    String test = "test";

    TestPolyInitialized(@UnknownInitialization String str) {
        this.testStr = str;
    }

    @PolyInitialized
    String identity(@PolyInitialized String str) {
        return str;
    }

    void test1() {
        identity(testStr);
    }

    void test2() {
        identity(test);
    }
}
