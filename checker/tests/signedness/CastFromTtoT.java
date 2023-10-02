import org.checkerframework.checker.signedness.qual.*;

import java.util.*;

class CastFromTtoT<T extends @UnknownSignedness Object> {
    @SuppressWarnings("unchecked")
    T bar(@UnknownSignedness Object p) {
        T x = (T) p;
        return x;
    }

    // Seems to have no cast, but in the instantiation, it's a downcast.
    // if we use getEffectiveAnnotations, we'll see it as no cast (qualifier aspect)
    // But it's fine, as compiler will warn about casting object to 'T' [unchecked warning]
    void foo(CastFromTtoT<@Signed Integer> s, @UnknownSignedness Object local) {
        @Signed Integer x = s.bar(local);
    }

    class Inner<T extends @UnknownSignedness Object> {
        T bar2(@Signed T p) {
            // without a warning from the compiler. The T in the casting expression may have
            // different signedness annotation with the arugment p.
            // :: warning: (cast.unsafe)
            T x = (T) p;
            return x;
        }

        // Looks like an upcast, but it's a downcast.
        // This time, as argument p has type T, which is the same as the casting type
        // and the compiler will not issue a warning. So we should give a warning.
        void foo2(Inner<@SignednessGlb Integer> s, @Signed Integer local) {
            @SignednessGlb Integer x = s.bar2(local);
        }
    }
}
