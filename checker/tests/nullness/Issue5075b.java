import org.checkerframework.checker.nullness.qual.Nullable;

class Issue5075b {
    class CExpl<V extends @Nullable Object> {
        I<V> c(N n) {
            return h(n.i());
        }

        abstract class N {
            abstract I<V> i();
        }

        static <V> I<V> h(I<V> i) {
            return i;
        }
    }

    class CImpl<V extends @Nullable Object> {
        I<V> c(N n) {
            return h(n.i());
        }

        abstract class N {
            abstract I<V> i();
        }

        static <V> I<V> h(I<V> i) {
            return i;
        }
    }

    interface I<V> {}
}
