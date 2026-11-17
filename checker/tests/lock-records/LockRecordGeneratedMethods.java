import org.checkerframework.checker.lock.qual.GuardedBy;

import java.util.concurrent.locks.ReentrantLock;

// @below-java16-jdk-skip-test
class LockRecordGeneratedMethods {
    record Item(String name) {}

    final ReentrantLock lock = new ReentrantLock();

    void test(@GuardedBy("lock") Item item, @GuardedBy("lock") Object o) {
        lock.lock();
        item.equals(o);
        item.hashCode();
        item.toString();
    }

    void lockNotHeld(@GuardedBy("lock") Item item) {
        // :: error: (lock.not.held)
        item.hashCode();
    }
}
