import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

@SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
public class Issue282 {
    @H1S1 Issue282() {}

    public class Inner {
        Inner(@H2S2 Issue282 Issue282.this) {}
    }

    public void test1() {
        // The enclosing type is @H1S1 @H2Top
        //        Inner inner = new Issue282().new Inner() {};
        // The enclosing type is @H1Top @H2Top
        new Inner();
    }

    //    class TestConstructorParameter {
    //
    //        // :: warning: (inconsistent.constructor.type) :: error: (super.invocation.invalid)
    //        TestConstructorParameter(Object p) {
    //            @H1S1 Object l1 = p;
    //            // :: error: (assignment.type.incompatible)
    //            @H1S2 Object l2 = p;
    //            Object l3 = p;
    //        }
    //
    //        void call() {
    //            // :: warning: (cast.unsafe.constructor.invocation)
    //            new TestConstructorParameter(new @H1S1 Object());
    //        }
    //    }
}
