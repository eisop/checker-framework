// A simpler test that @CreatesMustCallFor works as intended wrt the Resource Leak Checker.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("a")
class CreatesMustCallForSimpler {

    @CreatesMustCallFor
    void reset() {}

    @CreatesMustCallFor("this")
    void resetThis() {}

    void a() {}

    static @MustCall({}) CreatesMustCallForSimpler makeNoMC() {
        return null;
    }

    static void test1() {
        // :: error: required.method.not.called
        CreatesMustCallForSimpler cos = makeNoMC();
        @MustCall({}) CreatesMustCallForSimpler a = cos;
        cos.reset();
        // :: error: assignment.type.incompatible
        @CalledMethods({"reset"}) CreatesMustCallForSimpler b = cos;
        @CalledMethods({}) CreatesMustCallForSimpler c = cos;
    }
}
