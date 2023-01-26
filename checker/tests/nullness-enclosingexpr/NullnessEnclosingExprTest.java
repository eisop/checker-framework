import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class NullnessEnclosingExprTest {
    class Inner {
        Inner() {
            NullnessEnclosingExprTest.this.f.hashCode();
        }
    }

    class InnerFalsePositive {
        // This constructor does nothing, but the default type of the implicit enclosing expr is
        // @UnknownInitialization, so it is a false positive in line #30
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
        // False positive
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerFalsePositive();
        this.new InnerWithExplicitEnclosingExpression1();
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerWithExplicitEnclosingExpression2();
        f = "a";
    }

    Object f;
}
