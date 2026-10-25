// Test that a dependent type annotation on a varargs parameter is viewpoint-adapted at a call that
// passes the varargs elements individually.

import org.checkerframework.checker.lock.qual.GuardedBy;

public class VarargsDependentType {
    static void guarded(Object lock, @GuardedBy("#1") Object... os) {}

    void caller(Object lock, Object other, @GuardedBy("#1") Object g, @GuardedBy("#2") Object h) {
        guarded(lock);
        guarded(lock, g);
        guarded(lock, g, g);

        // :: error: (argument.type.incompatible)
        guarded(lock, h);
        // :: error: (argument.type.incompatible)
        guarded(other, g);
    }
}
