/*
 * @test
 *
 * @summary A VarHandle on a reference-typed field accepts null, which the annotated JDK
 * conservatively forbids because the same access mode on a primitive-typed field would throw.
 * -Astubs=sometimes-nullable.astub opts in to the unsound-but-convenient reading.  Exercises
 * every access mode that a reference-typed field supports and that takes a value.
 *
 * @compile/fail/ref=VarHandleNull.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker VarHandleNull.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=sometimes-nullable.astub VarHandleNull.java
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VarHandleNull {
    volatile ConcurrentMap<Object, Object> field = new ConcurrentHashMap<>();

    static final VarHandle HANDLE;

    static {
        try {
            HANDLE =
                    MethodHandles.lookup()
                            .findVarHandle(VarHandleNull.class, "field", ConcurrentMap.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    void set(ConcurrentMap<Object, Object> value) {
        HANDLE.set(this, null);
        HANDLE.setVolatile(this, null);
        HANDLE.setRelease(this, null);
        HANDLE.setOpaque(this, null);
    }

    void compareAndSet(ConcurrentMap<Object, Object> value) {
        HANDLE.compareAndSet(this, null, value);
        HANDLE.weakCompareAndSet(this, null, value);
        HANDLE.weakCompareAndSetPlain(this, null, value);
        HANDLE.weakCompareAndSetAcquire(this, null, value);
        HANDLE.weakCompareAndSetRelease(this, null, value);
    }

    void exchange(ConcurrentMap<Object, Object> value) {
        HANDLE.compareAndExchange(this, null, value);
        HANDLE.compareAndExchangeAcquire(this, null, value);
        HANDLE.compareAndExchangeRelease(this, null, value);
        HANDLE.getAndSet(this, null);
        HANDLE.getAndSetAcquire(this, null);
        HANDLE.getAndSetRelease(this, null);
    }
}
