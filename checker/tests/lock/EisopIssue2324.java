// Test case for https://github.com/eisop/checker-framework/issues/2324 :
// a method called in a lock expression must be pure, not only deterministic. A deterministic method
// that is not side-effect-free may change the heap, so two evaluations of the lock expression can
// name different locks.

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.MayReleaseLocks;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;

import java.util.concurrent.locks.ReentrantLock;

public class EisopIssue2324 {
    static class DeterministicLock {
        // :: error: (lock.expression.not.final)
        @GuardedBy("lock()") Object x = new Object();

        @SuppressWarnings("purity.not.deterministic.object.creation")
        @Deterministic
        Object lock() {
            return new Object();
        }

        void t() {
            // :: error: (lock.expression.not.final)
            synchronized (lock()) {
                // The type of x contains the same lock expression.
                // :: error: (expression.unparsable.type.invalid)
                x.hashCode();
            }
        }
    }

    // The same for an explicit lock.
    static class DeterministicExplicitLock {
        // :: error: (lock.expression.not.final)
        @GuardedBy("lock()") Object x = new Object();

        @SuppressWarnings("purity.not.deterministic.object.creation")
        @Deterministic
        ReentrantLock lock() {
            return new ReentrantLock();
        }

        @MayReleaseLocks
        void t() {
            // :: error: (lock.expression.not.final)
            lock().lock();
            // :: error: (expression.unparsable.type.invalid)
            x.hashCode();
            // :: error: (lock.expression.not.final)
            lock().unlock();
        }
    }

    static class PureExplicitLock {
        final ReentrantLock theLock = new ReentrantLock();

        @GuardedBy("lock()") Object x = new Object();

        @Pure
        ReentrantLock lock() {
            return theLock;
        }

        @MayReleaseLocks
        void t() {
            lock().lock();
            x.hashCode();
            lock().unlock();
        }
    }

    static class PureLock {
        final Object theLock = new Object();

        @GuardedBy("lock()") Object x = new Object();

        @Pure
        Object lock() {
            return theLock;
        }

        void t() {
            synchronized (lock()) {
                x.hashCode();
            }
        }
    }
}
