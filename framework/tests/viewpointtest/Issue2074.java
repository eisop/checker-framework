import viewpointtest.quals.*;

public class Issue2074 {
    @ReceiverDependentQual Object f;

    static void set(@PolyVP Issue2074 c, @PolyVP Object o) {
        // PolyVP may be instantiated to Top, so PolyVP |> ReceiverDependentQual == Lost.
        // :: error: (assignment.type.incompatible)
        c.f = o;
    }

    static @Top Object getTop(@PolyVP Issue2074 c) {
        // Reading c.f yields @Lost Object, which is a subtype of @Top Object.
        return c.f;
    }

    static @PolyVP Object getPoly(@PolyVP Issue2074 c) {
        // PolyVP |> ReceiverDependentQual == Lost, not PolyVP.
        // :: error: (return.type.incompatible)
        return c.f;
    }

    static void test(@A Issue2074 a, @B Object b) {
        set(a, b);
        @A Object aObj = a.f;
    }
}
