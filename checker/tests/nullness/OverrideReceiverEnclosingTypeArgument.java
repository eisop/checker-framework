// A type argument of an overrider receiver's enclosing type is fixed by the overridden class when
// the superclass of the inner class names that type parameter, as V in Inner extends Coll<V>.
// Restating the overridden receiver's type argument there is valid, narrowing it is not.

import org.checkerframework.checker.nullness.qual.*;

public class OverrideReceiverEnclosingTypeArgument {
    static class Coll<E extends @Nullable Object> {
        void m(Coll<@NonNull E> this) {}
    }

    static class Outer<K extends @Nullable Object, V extends @Nullable Object> {
        class Restates extends Coll<V> {
            @Override
            void m(Outer<K, @NonNull V>.Restates this) {}
        }

        class Unannotated extends Coll<V> {
            @Override
            void m(Outer<K, V>.Unannotated this) {}
        }

        class NarrowsUnpinned extends Coll<V> {
            @Override
            // :: error: (override.receiver.invalid)
            void m(Outer<@NonNull K, @NonNull V>.NarrowsUnpinned this) {}
        }
    }

    static class WideColl<E extends @Nullable Object> {
        void m(WideColl<@Nullable E> this) {}
    }

    static class WideOuter<V extends @Nullable Object> {
        class Narrows extends WideColl<V> {
            @Override
            // :: error: (override.receiver.invalid)
            void m(WideOuter<@NonNull V>.Narrows this) {}
        }
    }
}
