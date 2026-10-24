import org.checkerframework.checker.lock.qual.GuardedBy;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

// Enhanced-for loops and try-with-resources create artificial trees in the CFG. The @GuardedBy
// expressions in their types name local variables and must be resolved in the scope of the
// statement.
public class LockArtificial {
    final Object fieldLock = new Object();

    void locals(@GuardedBy("#2") List<Object> p, Object plock) {
        final Object lock = new Object();
        @GuardedBy("lock") List<Object> l = new ArrayList<>();
        synchronized (lock) {
            for (Object o : l) {}
        }
        synchronized (plock) {
            for (Object o : p) {}
        }
    }

    void field() {
        @GuardedBy("fieldLock") List<Object> l = new ArrayList<>();
        @GuardedBy("this.fieldLock") List<Object> m = new ArrayList<>();
        synchronized (fieldLock) {
            for (Object o : l) {}
            for (Object o : m) {}
        }
    }

    void tryWithResources() throws Exception {
        final Object lock = new Object();
        synchronized (lock) {
            try (@GuardedBy("lock") StringReader r = new StringReader("")) {}
        }
    }

    void lambda() {
        final Object lock = new Object();
        Runnable r =
                () -> {
                    @GuardedBy("lock") List<Object> l = new ArrayList<>();
                    synchronized (lock) {
                        for (Object o : l) {}
                    }
                };
    }

    void anonymous() {
        final Object lock = new Object();
        new Object() {
            void m() {
                @GuardedBy("lock") List<Object> l = new ArrayList<>();
                synchronized (lock) {
                    for (Object o : l) {}
                }
            }
        };
    }
}
