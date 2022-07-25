import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Issue282 {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1S1 Issue282() {}

    public class Inner {}

    public void test1() {
        // :: error: (constructor.invocation.invalid)
        Inner inner = new @H1S2 Issue282().new Inner();
    }
}
