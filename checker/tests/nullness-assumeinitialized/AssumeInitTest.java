import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class AssumeInitTest {

    AssumeInitTest f;

    public AssumeInitTest(String arg) {}

    void test() {
        @NonNull String s = "234";
        // :: error: (assignment.type.incompatible)
        s = null;
    }

    void test2(@UnknownInitialization @NonNull AssumeInitTest t) {
        @Initialized @NonNull AssumeInitTest a = t.f;
    }

    void simplestTestEver() {
        @NonNull String a = "abc";

        // :: error: (assignment.type.incompatible)
        a = null;

        // :: error: (assignment.type.incompatible)
        @NonNull String b = null;
    }
}
