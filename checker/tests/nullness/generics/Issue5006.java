public class Issue5006 {

    static class C<T> {
        T get() {
            throw new RuntimeException("");
        }
    }

    interface X {
        C<? extends Object> get();
    }

    interface Y extends X {
        @Override
        // C<? super @NonNull Object> can be a C<@Nullable Object>, whose get() returns null, so
        // it is not a subtype of C<? extends @NonNull Object>.
        // :: error: (override.return.invalid)
        C<? super Object> get();
    }
}
