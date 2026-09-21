import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class WildcardBounds {

    abstract class OuterNbl<T extends @Nullable Object> {
        abstract T get();

        abstract class Inner<U extends T> {
            abstract U get();

            abstract class Chain<V extends U, W extends V> {
                abstract W get();
            }

            Object m0(Chain<? extends Object, ? extends Object> p) {
                return p.get();
            }

            Object m1(Chain<? extends @NonNull T, ? extends @NonNull T> p) {
                return p.get();
            }

            Object m2(Chain<?, ?> p) {
                // :: error: (return.type.incompatible)
                return p.get();
            }

            Object m3(Chain<? extends @Nullable Object, ? extends @Nullable Object> p) {
                // :: error: (return.type.incompatible)
                return p.get();
            }

            Object m4(Chain<@NonNull ?, @NonNull ?> p) {
                return p.get();
            }

            void callsNonNull(
                    OuterNbl<Object>.Inner<Number> i,
                    OuterNbl<Object>.Inner<Number>.Chain<Integer, Integer> n) {
                i.m0(n);
                i.m1(n);
                i.m2(n);
                i.m3(n);
                i.m4(n);
            }

            void callsNullable(
                    OuterNbl<@Nullable Object>.Inner<@Nullable Number> i,
                    OuterNbl<@Nullable Object>.Inner<@Nullable Number>.Chain<
                                    @Nullable Integer, @Nullable Integer>
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

        Object m0(Inner<? extends Object> p) {
            return p.get();
        }

        Object m1(Inner<? extends @NonNull T> p) {
            return p.get();
        }

        Object m2(Inner<?> p) {
            // :: error: (return.type.incompatible)
            return p.get();
        }

        Object m3(Inner<? extends @Nullable Object> p) {
            // :: error: (return.type.incompatible)
            return p.get();
        }

        Object m4(Inner<@NonNull ?> p) {
            return p.get();
        }

        // We could add calls for these methods.
    }

    Object m0(OuterNbl<? extends Object> p) {
        return p.get();
    }

    Object m1(OuterNbl<? extends @NonNull Object> p) {
        return p.get();
    }

    Object m2(OuterNbl<?> p) {
        // :: error: (return.type.incompatible)
        return p.get();
    }

    Object m3(OuterNbl<? extends @Nullable Object> p) {
        // :: error: (return.type.incompatible)
        return p.get();
    }

    Object m4(OuterNbl<@NonNull ?> p) {
        return p.get();
    }

    void callsOuter(OuterNbl<String> s, OuterNbl<@Nullable String> ns) {
        m0(s);
        m1(s);
        m2(s);
        m3(s);
        m4(s);

        // :: error: (argument.type.incompatible)
        m0(ns);
        // :: error: (argument.type.incompatible)
        m1(ns);
        // OK
        m2(ns);
        // OK
        m3(ns);
        // :: error: (argument.type.incompatible)
        m4(ns);
    }

    // We could add an OuterNonNull to also test with a non-null upper bound.
    // But we probably already test that enough.
}
