import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class PolyMethodReceiver<T, U> {

    static class Box<T> {
        void methodPoly(@H1Poly Box<@H1Poly T> this) {}

        void methodNonPoly(@H1Poly Box<@H1Top T> this) {}
    }

    void testWildcard(
            Box<?> wildcardBox, Box<? extends String> extendsBox, Box<? super String> superBox) {
        wildcardBox.methodPoly();
        // :: error: (method.invocation.invalid)
        wildcardBox.methodNonPoly();

        extendsBox.methodPoly();
        // :: error: (method.invocation.invalid)
        extendsBox.methodNonPoly();

        superBox.methodPoly();
        // :: error: (method.invocation.invalid)
        superBox.methodNonPoly();
    }

    void testTypeVar(Box<T> typevarBox) {
        typevarBox.methodPoly();
        // :: error: (method.invocation.invalid)
        typevarBox.methodNonPoly();
    }

    void testDeclared(Box<@H1Bot String> declaredBox, Box<@H1Top String> topBox) {
        // :: error: (method.invocation.invalid)
        declaredBox.methodPoly();
        // :: error: (method.invocation.invalid)
        declaredBox.methodNonPoly();

        topBox.methodNonPoly();
    }

    static class BoxMulti<T, U> {
        void methodPolyFirst(@H1Poly BoxMulti<@H1Poly T, @H1Top U> this) {}

        void methodPolySecond(@H1Poly BoxMulti<@H1Top T, @H1Poly U> this) {}

        void methodPolyBoth(@H1Poly BoxMulti<@H1Poly T, @H1Poly U> this) {}

        void methodPolyNone(@H1Poly BoxMulti<@H1Top T, @H1Top U> this) {}
    }

    void testMultiWildcards(BoxMulti<?, ?> wBoth, BoxMulti<T, ?> wSecond, BoxMulti<?, U> wFirst) {
        wBoth.methodPolyBoth();
        // wBoth replaces both, so only needs to check @H1Poly receiver
        // methodPolyFirst has @H1Top U, but wBoth has ? for U. The substitution replaces U with ?.
        // Wait, does substitution happen for NON-poly args? NO.
        // So methodPolyFirst's second arg is @H1Top U. wBoth's second arg is ?. ? vs @H1Top U
        // fails.
        // :: error: (method.invocation.invalid)
        wBoth.methodPolyFirst();
        // :: error: (method.invocation.invalid)
        wBoth.methodPolySecond();
        // :: error: (method.invocation.invalid)
        wBoth.methodPolyNone();

        // wFirst has ?, U.
        wFirst.methodPolyBoth();
        // methodPolyFirst has @H1Poly T (substituted), @H1Top U. wFirst has ?, U. U is subtype of
        // @H1Top U. So this succeeds.
        wFirst.methodPolyFirst();
        // :: error: (method.invocation.invalid)
        wFirst.methodPolySecond();
        // :: error: (method.invocation.invalid)
        wFirst.methodPolyNone();

        wSecond.methodPolyBoth();
        // :: error: (method.invocation.invalid)
        wSecond.methodPolyFirst();
        // :: error: (method.invocation.invalid)
        wSecond.methodPolySecond();
        // :: error: (method.invocation.invalid)
        wSecond.methodPolyNone();
    }

    <V extends Box<?>> void testReceiverTypeParam(V typeParamBox) {
        // Here typeParamBox is a type parameter V whose upper bound is Box<?>
        typeParamBox.methodPoly();
        // :: error: (method.invocation.invalid)
        typeParamBox.methodNonPoly();
    }

    static class BoxDual<T, U> {
        void methodH1PassH2Fail(@H1Poly @H2Top BoxDual<@H1Poly T, @H2Bot U> this) {}

        void methodH1FailH2Pass(@H1Top @H2Poly BoxDual<@H1Bot T, @H2Poly U> this) {}

        void methodH1PolyH2Poly(@H1Poly @H2Poly BoxDual<@H1Poly T, @H2Poly U> this) {}
    }

    void testH1H2(BoxDual<?, ?> wBoth) {
        // H1 check: declared is @H1Poly T -> substituted, passes!
        // H2 check: declared is @H2Bot U -> not poly, ? is not a subtype of @H2Bot U -> fails!
        // :: error: (method.invocation.invalid)
        wBoth.methodH1PassH2Fail();

        // H1 check: declared is @H1Bot T -> not poly, ? is not a subtype of @H1Bot T -> fails!
        // H2 check: declared is @H2Poly U -> substituted, passes!
        // :: error: (method.invocation.invalid)
        wBoth.methodH1FailH2Pass();

        // Both are polymorphic -> both substituted -> both pass!
        wBoth.methodH1PolyH2Poly();
    }
}
