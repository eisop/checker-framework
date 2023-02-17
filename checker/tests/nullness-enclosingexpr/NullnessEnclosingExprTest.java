import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class NullnessEnclosingExprTest {
    class Inner {
        Inner() {
            // This will lead to a NPE at line #35, since NullnessEnclosingExprTest
            // is not intialized yet.
            NullnessEnclosingExprTest.this.f.hashCode();
        }
    }

    class InnerWithImplicitEnclosingExpression {
        // The default annotation type of the implicit enclosing expression is
        // @UnknownInitialization, so we will get an error at line #37
        InnerFalsePositive() {}
    }

    class InnerWithExplicitEnclosingExpression1 {
        InnerWithExplicitEnclosingExpression1(
                @UnknownInitialization NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {
            // This will NOT lead to a NPE as we annotate @UnknownInitialization to the enclosing
            // expression
            NullnessEnclosingExprTest.this.f.hashCode();
        }
    }

    class InnerWithExplicitEnclosingExpression2 {
        InnerWithExplicitEnclosingExpression2(
                @Initialized NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {}
    }

    NullnessEnclosingExprTest() {
        // :: error: (enclosingexpr.type.incompatible)
        this.new Inner();
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerWithImplicitEnclosingExpression();
        this.new InnerWithExplicitEnclosingExpression1();
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerWithExplicitEnclosingExpression2();
        f = "a";
    }

    Object f;
}
