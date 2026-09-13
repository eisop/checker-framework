import viewpointtest.quals.*;

public class Issue2074 {
    @ReceiverDependentQual Object f;

    static void set(@PolyVP Issue2074 c, @PolyVP Object o) {
        // PolyVP may be instantiated to Top, so PolyVP |> ReceiverDependentQual == Lost.
        // :: error: (assignment.type.incompatible)
        c.f = o;
    }

    static void test(@A Issue2074 a, @B Object b) {
        set(a, b);
        @A Object aObj = a.f;
    }
}
