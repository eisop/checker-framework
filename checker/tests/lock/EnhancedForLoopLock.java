import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;

import java.util.ArrayList;
import java.util.List;

public class EnhancedForLoopLock {
    final Object lock = new Object();

    final Object otherLock = new Object();

    @GuardedBy("lock") List<Object> field;

    Object @GuardedBy("lock") [] array;

    List<Object> unguardedField;

    @GuardedBy("lock") List<Object> getList() {
        return field;
    }

    void notHeld(@GuardedBy("lock") List<Object> param) {
        // :: error: (lock.not.held)
        for (Object o : field) {}
        // :: error: (lock.not.held)
        for (Object o : param) {}
        // :: error: (lock.not.held)
        for (Object o : array) {}
        // :: error: (lock.not.held)
        for (Object o : getList()) {}
        List<Object> local = field;
        // :: error: (lock.not.held)
        for (Object o : local) {}
    }

    void held(@GuardedBy("lock") List<Object> param) {
        synchronized (lock) {
            for (Object o : field) {}
            for (Object o : param) {}
            for (Object o : array) {}
            for (Object o : getList()) {}
            List<Object> local = field;
            for (Object o : local) {}
        }
    }

    void otherLockHeld() {
        synchronized (otherLock) {
            // :: error: (lock.not.held)
            for (Object o : field) {}
        }
    }

    void nested(List<Object> list) {
        for (Object o : list) {
            // :: error: (lock.not.held)
            for (Object p : field) {}
            synchronized (lock) {
                for (Object p : field) {}
            }
        }
        synchronized (lock) {
            for (Object o : field) {
                for (Object p : field) {}
            }
        }
    }

    void unguarded(List<Object> list, Object[] arr) {
        for (Object o : list) {}
        for (Object o : arr) {}
        for (Object o : unguardedField) {}
        List<Object> local = new ArrayList<>();
        for (Object o : local) {}
    }

    void guardSatisfied(@GuardSatisfied List<Object> list) {
        for (Object o : list) {}
    }

    void guardedByUnknown(@GuardedByUnknown List<Object> list) {
        // :: error: (lock.not.held)
        for (Object o : list) {}
    }
}
