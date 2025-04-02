import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

@ReceiverDependentMutable public class StaticTest {
    // :: error: (static.receiverdependentmutable.forbidden)
    static @ReceiverDependentMutable Object rmdField = new @ReceiverDependentMutable Object();

    static {
        // :: error: (static.receiverdependentmutable.forbidden)
        @Readonly Object rdmObject = (@ReceiverDependentMutable Object) rmdField;
        // :: error: (static.receiverdependentmutable.forbidden)
        new @ReceiverDependentMutable Object();
    }

    // :: error: (static.receiverdependentmutable.forbidden)
    static @ReceiverDependentMutable Object staticMethod(
            // :: error: (static.receiverdependentmutable.forbidden)
            @ReceiverDependentMutable Object rdmObject) {
        return rmdField;
    }

    // :: error: (static.receiverdependentmutable.forbidden)
    static <T extends @ReceiverDependentMutable Object> void foo(T p) {
        // :: error: (static.receiverdependentmutable.forbidden)
        p = null;
    }

    @ReceiverDependentMutable static class RDMStaticClass {}

    void test() {
        new StaticTest.@Mutable RDMStaticClass();
        new StaticTest.@Immutable RDMStaticClass();
    }
}
