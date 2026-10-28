// A requalified type-variable use in an overridden method's receiver keeps its qualifier when
// the type variable is substituted with the overrider's own type variable, so that an overrider
// that restates the receiver is accepted and one that narrows it is rejected.

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
}
