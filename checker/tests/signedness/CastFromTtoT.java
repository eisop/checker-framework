import org.checkerframework.checker.signedness.qual.*;

import java.util.*;

class CastFromTtoT<T extends @UnknownSignedness Object> {
    @SuppressWarnings("unchecked")
    T bar(@UnknownSignedness Object p) {
        // Seems to have no cast in terms of the qualifier (from @UnknownSignedness to
        // @UnknownSignedness), but in instantiation, it could be a downcast.
        // See method foo below. It's okay not to report downcast warnings as Javac will warn about
        // casting object to 'T' (unchecked warning)
        T x = (T) p;
        return x;
    }

    void foo(CastFromTtoT<@Signed Integer> s, @UnknownSignedness Object local) {
        // Here, we passed in an @UnknownSignedness object and the method signature after
        // substitution is @Signed Integer bar(@UnknownSignedness Object). This makes the typecast
        // discussed earlier a downcast.
        @Signed Integer x = s.bar(local);
    }

    class Inner<T extends @UnknownSignedness Object> {
        T bar2(@Signed T p) {
            // The casting expression below looks like an upcast (in terms of the qualifier),
            // but it could be a downcast in invocation (See method foo2 below for an example).
            // We should report downcast warning if there is one because
            // Javac doesn't warn when casting a variable from type T to T.
            // :: warning: (cast.unsafe)
            T x = (T) p;
            return x;
        }

        void foo2(Inner<@SignednessGlb Integer> s, @Signed Integer local) {
            // Here, we passed in an @Signed integer and the method signature after
            // substitution is @SignednessGlb Integer bar2(@Signed Object). This makes the typecast
            // discussed in method bar2 a downcast.
            @SignednessGlb Integer x = s.bar2(local);
        }
    }
}
