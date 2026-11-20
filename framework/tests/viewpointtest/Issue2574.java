// Test case for eisop#2574: a receiver whose type arguments are the class's own type variables.
// Viewpoint adaptation adapts the qualifiers of the declared signature, and asMemberOf substitutes
// the receiver's type arguments for the class's type variables exactly once. A second substitution
// would turn `Swap<Y, X>` back into `Swap<X, Y>`. Each member below is invoked on, or overrides
// from, such a receiver and must type-check without errors.

import viewpointtest.quals.*;

public class Issue2574 {
    interface S5<T, R> {
        R apply(T t);

        default <V> T call(S5<? super V, T> before, V v) {
            return before.apply(v);
        }
    }

    static class Swap<X, Y> {
        X fa;
        Y fb;

        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        @ReceiverDependentQual Swap(X a, Y b) {
            fa = a;
            fb = b;
        }

        Y apply(X a) {
            return fb;
        }

        X call(Swap<Y, X> other, Y b) {
            return other.apply(b);
        }

        @A Swap<Y, X> flip() {
            return new @A Swap<Y, X>(fb, fa);
        }

        void fields(Swap<Y, X> other, X a, Y b) {
            Y x = other.fa;
            X y = other.fb;
            other.fa = b;
            other.fb = a;
        }

        class Inner extends Swap<Y, X> {
            @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
            @ReceiverDependentQual Inner(Y b, X a) {
                super(b, a);
            }

            @Override
            X apply(Y b) {
                return fb;
            }
        }
    }
}
