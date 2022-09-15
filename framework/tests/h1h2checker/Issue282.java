import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

@SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
public class Issue282 {
    @H1S1 Issue282() {}

    public class Inner {
        Inner(@H2S2 Issue282 Issue282.this) {}
    }

    public void test1() {
        // The enclosing type is @H1S1 @H2Top, the receiver type is @H1Top @H2Top
        // should it be from its super constructor or its instantiation?
        // :: error: (receiver.invalid)
        new Issue282().new Inner() {};
        // The enclosing type is @H1Top @H2Top, the receiver type is @H1Top @H2S2
        //      new Inner();
    }
}
