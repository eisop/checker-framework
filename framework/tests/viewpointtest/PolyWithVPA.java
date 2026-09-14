import viewpointtest.quals.*;

public class PolyWithVPA {
    static class PolyClass {
        @ReceiverDependentQual Object foo(@PolyVP Object o) {
            return null;
        }
    }

    static void test1(@A PolyClass a, @B Object bObj) {
        @A Object aObj = a.foo(bObj);
    }

    // only poly annos in decl are resolved
    static void test2(@PolyVP PolyClass poly, @B Object bObj) {
        // PolyVP |> ReceiverDependentQual == Lost
        // :: error: (assignment.type.incompatible)
        @PolyVP Object polyObj = poly.foo(bObj);
        // :: error: (assignment.type.incompatible)
        @B Object anotherBObj = poly.foo(bObj);
        @Top Object topObj = poly.foo(bObj);
        // Lost is not reflexive, so even assigning Lost to Lost is an error.
        // :: error: (assignment.type.incompatible)
        @Lost Object lostObj = poly.foo(bObj);
    }
}
