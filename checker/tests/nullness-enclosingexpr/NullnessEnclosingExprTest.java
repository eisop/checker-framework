import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class NullnessEnclosingExprTest {
    class Inner {
        Inner() {
            // This will lead to a NPE at line #31, since NullnessEnclosingExprTest
            // is not intialized yet.
            NullnessEnclosingExprTest.this.f.hashCode();
        }
    }

    class InnerFalsePositive {
        // Although this constructor does nothing, the default type of the implicit enclosing expr
        // is
        // @UnknownInitialization, so it will throw an error at line #31.
        InnerFalsePositive() {}
    }

    class InnerWithExplicitEnclosingExpression1 {
        InnerWithExplicitEnclosingExpression1(
                @UnknownInitialization NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {}
    }

    class InnerWithExplicitEnclosingExpression2 {
        InnerWithExplicitEnclosingExpression2(
                @Initialized NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {}
    }

    NullnessEnclosingExprTest() {
        // :: error: (enclosingexpr.type.incompatible)
        this.new Inner();
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerFalsePositive();
        this.new InnerWithExplicitEnclosingExpression1();
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerWithExplicitEnclosingExpression2();
        f = "a";
    }

    Object f;
}
