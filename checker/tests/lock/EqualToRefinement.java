import org.checkerframework.checker.lock.qual.GuardedBy;

// Two references to the same object may be guarded by different locks, so a comparison of the
// references does not refine the guard of either.
public class EqualToRefinement {
    final Object lockA = new Object();
    final Object lockB = new Object();

    void guards(@GuardedBy("lockA") Object x, @GuardedBy("lockB") Object y) {
        if (x != y) {
            return;
        }
        // :: error: (lock.not.held)
        x.toString();
        // :: error: (lock.not.held)
        y.toString();
    }

    void aliases() {
        Object o = new Object();
        @GuardedBy("lockA") Object x = o;
        @GuardedBy("lockB") Object y = o;
    }
}
