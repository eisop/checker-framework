// A requalified type-variable use in an overridden method's receiver keeps its qualifier when
// the type variable is substituted with the overrider's own type variable, so that an overrider
// that restates the receiver is accepted and one that narrows it is rejected. The same holds for
// a type argument of the receiver's enclosing type that the superclass fixes.

import viewpointtest.quals.*;

class RequalifiedReceiverTypeArgument {
    static class Base<T extends @Top Object> {
        void m(Base<@A T> this) {}
    }

    static class Restates<T extends @Top Object> extends Base<T> {
        @Override
        void m(Restates<@A T> this) {}
    }

    static class Narrows<T extends @Top Object> extends Base<T> {
        @Override
        // :: error: (override.receiver.invalid)
        void m(Narrows<@B T> this) {}
    }

    static class Outer<K extends @Top Object, V extends @Top Object> {
        class RestatesEnclosing extends Base<V> {
            @Override
            void m(Outer<K, @A V>.RestatesEnclosing this) {}
        }

        class NarrowsEnclosing extends Base<V> {
            @Override
            // :: error: (override.receiver.invalid)
            void m(Outer<K, @B V>.NarrowsEnclosing this) {}
        }

        class NarrowsUnpinned extends Base<V> {
            @Override
            // :: error: (override.receiver.invalid)
            void m(Outer<@A K, @A V>.NarrowsUnpinned this) {}
        }
    }
}
