/*
 * @test
 * @summary Test case for issue #6373: https://github.com/typetools/checker-framework/issues/6373
 *
 * @requires jdk.version.major >= 10
 * @compile/timeout=80 -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker Issue6373.java
 */
public class Issue6373 {

    abstract static class C1<
                    C extends C1<C, Q, B, D, CR>,
                    Q extends C2<C, Q, B, D, CR>,
                    B extends C3<C, Q, B, D, CR>,
                    D extends C4<C, Q, B, D, CR>,
                    CR extends C5<CR>>
            extends C6 {}

    static class C6 {}

    abstract static class C2<
                    C extends C1<C, Q, B, D, RT>,
                    Q extends C2<C, Q, B, D, RT>,
                    B extends C3<C, Q, B, D, RT>,
                    D extends C4<C, Q, B, D, RT>,
                    RT extends C5<RT>>
            implements C7 {}

    abstract static class C3<
            C extends C1<C, Q, B, D, R>,
            Q extends C2<C, Q, B, D, R>,
            B extends C3<C, Q, B, D, R>,
            D extends C4<C, Q, B, D, R>,
            R extends C5<R>> {}

    abstract static class C4<
            C extends C1<C, Q, B, D, R>,
            Q extends C2<C, Q, B, D, R>,
            B extends C3<C, Q, B, D, R>,
            D extends C4<C, Q, B, D, R>,
            R extends C5<R>> {
        interface I<T> {}
    }

    abstract static class C5<R2 extends C5<R2>> implements C7 {}

    interface C7 {}

    abstract static class C8<
            C extends C1<C, Q, B, D, CR>,
            Q extends C2<C, Q, B, D, CR>,
            B extends C3<C, Q, B, D, CR>,
            D extends C4<C, Q, B, D, CR>,
            CR extends C5<CR>,
            RpT extends C5<RpT>> {

        public static <
                        C extends C1<C, Q, B, D, CR>,
                        Q extends C2<C, Q, B, D, CR>,
                        B extends C3<C, Q, B, D, CR>,
                        D extends C4<C, Q, B, D, CR>,
                        CR extends C5<CR>,
                        RpT extends C5<RpT>>
                Builder<C, Q, B, D, CR, RpT> n(Q q) {
            throw new AssertionError();
        }

        abstract static class Builder<
                C extends C1<C, Q, B, D, CR>,
                Q extends C2<C, Q, B, D, CR>,
                B extends C3<C, Q, B, D, CR>,
                D extends C4<C, Q, B, D, CR>,
                CR extends C5<CR>,
                RpT extends C5<RpT>> {

            public abstract Builder<C, Q, B, D, CR, RpT> f(C9<?> p);

            public C8<C, Q, B, D, CR, RpT> b() {
                throw new AssertionError();
            }
        }
    }

    abstract static class C9<W extends C9<W>> {}

    static final class C10 extends C9<C10> {}

    abstract static class C11<W extends C9<W>, B extends C11<W, B>> {}

    static final class C12 extends C11<C10, C12> {

        public C10 b() {
            throw new AssertionError();
        }
    }

    static class C13 {

        public static final C12 n() {
            return new C12();
        }

        static final class C14 extends C1<C14, C15, C16, C17, C18> {}

        static final class C15 extends C2<C14, C15, C16, C17, C18> {}

        static final class C18 extends C5<C18> {}

        static class C17 extends C4<C14, C15, C16, C17, C18> implements C4.I<Long>, C19 {}

        static final class C16 extends C3<C14, C15, C16, C17, C18> {

            public C15 b() {
                throw new AssertionError();
            }
        }

        static final C16 q() {
            throw new AssertionError();
        }
    }

    interface C19 {}

    void f() {
        var x = C8.n(C13.q().b()).f(C13.n().b()).b();
        var y = x;
        x = y;
        x = y;
        x = y;
    }

    void g() {
        Object x = C8.n(C13.q().b()).f(C13.n().b()).b();
        Object y = x;
        x = y;
        x = y;
        x = y;
    }
}
