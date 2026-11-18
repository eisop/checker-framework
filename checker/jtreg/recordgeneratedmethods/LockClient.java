import org.checkerframework.checker.lock.qual.GuardedBy;

import java.util.concurrent.locks.ReentrantLock;

public class LockClient {
    final ReentrantLock lock = new ReentrantLock();

    void test(RecordLib.@GuardedBy("lock") Item item, @GuardedBy("lock") Object o) {
        lock.lock();
        item.equals(o);
        item.hashCode();
        item.toString();
    }
}
