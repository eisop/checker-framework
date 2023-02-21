import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class NullnessEnclosingExprTest {
    class InnerWithImplicitEnclosingExpression {
        // This will lead to a NPE at line #27, since NullnessEnclosingExprTest
        // is not intialized yet.
        InnerWithImplicitEnclosingExpression() {
            NullnessEnclosingExprTest.this.f.hashCode();
        }
    }

    class InnerWithUnknownInitializationEnclosingExpression {
        InnerWithUnknownInitializationEnclosingExpression(
                @UnknownInitialization NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {
            // TODO: This SHOULD lead to a NPE as we annotate @UnknownInitialization to the
            // enclosing expression
            NullnessEnclosingExprTest.this.f.hashCode();
        }
    }

    class InnerWithInitializedEnclosingExpression {
        // The default type of enclosing expression is same as InnerWithImplicitEnclosingExpression,
        // we just make it explicit for testing.
        InnerWithInitializedEnclosingExpression(
                @Initialized NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {}
    }

    NullnessEnclosingExprTest() {
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerWithImplicitEnclosingExpression();
        this.new InnerWithUnknownInitializationEnclosingExpression();
        // :: error: (enclosingexpr.type.incompatible)
        this.new InnerWithInitializedEnclosingExpression();
        f = "a";
    }

    Object f;
}
