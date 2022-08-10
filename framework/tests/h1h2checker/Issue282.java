import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Issue282 {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    @H1S1 Issue282() {}

    public class Inner {
        Inner(@H2S2 Issue282 Issue282.this) {}
        ;
    }

    public void test1() {
        Inner inner = new Issue282().new Inner();
    }
}
