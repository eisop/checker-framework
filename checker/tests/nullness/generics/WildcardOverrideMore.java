import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class WildcardOverrideMore {
    interface Box<X extends @Nullable Object> {}

    interface Super<T extends @Nullable Object> {
        <U extends T> void foo(Box<? extends U> lib);

        <U extends T> Box<? extends U> retfoo();

        <U extends T> void bar(Box<? super U> lib);

        <U extends T> Box<? super U> retbar();
    }

    interface Sub<V extends @Nullable Object> extends Super<V> {
        @Override
        <W extends V> void foo(Box<? extends W> lib);

        @Override
        <W extends V> Box<? extends W> retfoo();

        @Override
        <W extends V> void bar(Box<? super W> lib);

        @Override
        <W extends V> Box<? super W> retbar();
    }

    interface SubErrors<V extends @Nullable Object> extends Super<V> {
        @Override
        // :: error: (override.param.invalid)
        <W extends V> void foo(Box<? extends @NonNull W> lib);

        @Override
        // :: error: (override.return.invalid)
        <W extends V> Box<? extends @Nullable W> retfoo();

        @Override
        // :: error: (override.param.invalid)
        <W extends V> void bar(Box<@NonNull ? super @NonNull W> lib);

        @Override
        // :: error: (override.return.invalid)
        <W extends V> Box<@Nullable ? super @NonNull W> retbar();
    }
}
