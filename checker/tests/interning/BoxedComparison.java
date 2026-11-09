// A comparison that is boxed, by an assignment to a Boolean, a return from a method that returns
// Boolean, or an argument, is the argument of a synthetic Boolean.valueOf call. Checking that call
// reports the comparison once and does not visit it again from outside its method, where the
// Interning Checker's heuristics could not walk its enclosing statements.
public class BoxedComparison {
    Boolean assignBoxes(Object x, Object y) {
        // :: error: (not.interned)
        Boolean b = x == y;
        return b;
    }

    Boolean returnBoxes(Object x, Object y) {
        // :: error: (not.interned)
        return x == y;
    }

    Boolean argumentBoxes(Object x, Object y) {
        // :: error: (not.interned)
        return identity(x == y);
    }

    static Boolean identity(Boolean b) {
        return b;
    }
}
