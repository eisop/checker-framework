import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;

class WildcardBounds {

    abstract class OuterTop<T extends @H1Top Object> {
        abstract T get();

        abstract class Inner<U extends T> {
            abstract U get();

            abstract class Chain<V extends U, W extends V> {
                abstract W get();
            }

            @H1S1 Object m0(Chain<? extends @H1S1 Object, ? extends @H1S1 Object> p) {
                return p.get();
            }

            @H1S1 Object m1(Chain<? extends @H1S1 T, ? extends @H1S1 T> p) {
                return p.get();
            }

            @H1S1 Object m2(Chain<?, ?> p) {
                // :: error: (return.type.incompatible)
                return p.get();
            }

            @H1S1 Object m3(Chain<? extends @H1Top Object, ? extends @H1Top Object> p) {
                // :: error: (return.type.incompatible)
                return p.get();
            }

            @H1S1 Object m4(Chain<@H1S1 ?, @H1S1 ?> p) {
                return p.get();
            }

            void callsS1(
                    OuterTop<@H1S1 Object>.Inner<@H1S1 Number> i,
                    OuterTop<@H1S1 Object>.Inner<@H1S1 Number>.Chain<@H1S1 Integer, @H1S1 Integer>
                            n) {
                i.m0(n);
                i.m1(n);
                i.m2(n);
                i.m3(n);
                i.m4(n);
            }

            void callsTop(
                    OuterTop<@H1Top Object>.Inner<@H1Top Number> i,
                    OuterTop<@H1Top Object>.Inner<@H1Top Number>.Chain<
                                    @H1Top Integer, @H1Top Integer>
                            n) {
                // :: error: (argument.type.incompatible)
                i.m0(n);
                // :: error: (argument.type.incompatible)
                i.m1(n);
                // OK
                i.m2(n);
                // OK
                i.m3(n);
                // :: error: (argument.type.incompatible)
                i.m4(n);
            }
        }

        @H1S1 Object m0(Inner<? extends @H1S1 Object> p) {
            return p.get();
        }

        @H1S1 Object m1(Inner<? extends @H1S1 T> p) {
            return p.get();
        }

        @H1S1 Object m2(Inner<?> p) {
            // :: error: (return.type.incompatible)
            return p.get();
        }

        @H1S1 Object m3(Inner<? extends @H1Top Object> p) {
            // :: error: (return.type.incompatible)
            return p.get();
        }

        @H1S1 Object m4(Inner<@H1S1 ?> p) {
            return p.get();
        }

        // We could add calls for these methods.
    }

    @H1S1 Object m0(OuterTop<? extends @H1S1 Object> p) {
        return p.get();
    }

    @H1S1 Object m1(OuterTop<?> p) {
        // :: error: (return.type.incompatible)
        return p.get();
    }

    @H1S1 Object m2(OuterTop<? extends @H1Top Object> p) {
        // :: error: (return.type.incompatible)
        return p.get();
    }

    @H1S1 Object m3(OuterTop<@H1S1 ?> p) {
        return p.get();
    }

    void callsOuter(OuterTop<@H1S1 String> s, OuterTop<@H1Top String> ns) {
        m0(s);
        m1(s);
        m2(s);
        m3(s);

        // :: error: (argument.type.incompatible)
        m0(ns);
        // OK
        m1(ns);
        // OK
        m2(ns);
        // :: error: (argument.type.incompatible)
        m3(ns);
    }

    // We could add an OuterS1 to also test with a non-top upper bound.
    // But we probably already test that enough.
}
