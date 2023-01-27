import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Issue282 {
    // Declared constructor type is not consistent with default from class.
    @SuppressWarnings({"super.invocation.invalid", "inconsistent.constructor.type"})
    @H1S1 Issue282() {}

    public class Inner {
        Inner(@H2S2 Issue282 Issue282.this) {}
    }

    public void test1() {
        // The enclosing type is @H1S1 @H2Top, the receiver type is @H1Top @H2Top
        // :: error: (enclosingexpr.type.incompatible)
        Inner inner = new Issue282().new Inner() {};
        // :: error: (enclosingexpr.type.incompatible)
        Inner inner2 = new Issue282().new Inner();
    }

    class Issue282Sub extends Issue282 {}

    public void test2() {
        // found: @H1Top @H2Top Issue282.@H1Top @H2Top Issue282Sub. required: @H1Top @H2S2 Issue282
        // :: error: (enclosingexpr.type.incompatible)
        Inner inner = new Issue282Sub().new Inner();
    }
}