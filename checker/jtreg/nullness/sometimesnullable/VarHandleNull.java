/*
 * @test
 *
 * @summary A VarHandle on a reference-typed field accepts null, which the annotated JDK
 * conservatively forbids because the same method on a primitive-typed field would throw.
 * -Astubs=sometimes-nullable.astub opts in to the unsound-but-convenient reading.
 *
 * @compile/fail/ref=VarHandleNull.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker VarHandleNull.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=sometimes-nullable.astub VarHandleNull.java
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ConcurrentMap;

public class VarHandleNull {
    volatile ConcurrentMap<Object, Object> refreshes =
            new java.util.concurrent.ConcurrentHashMap<>();
    static final VarHandle REFRESHES;

    static {
        try {
            REFRESHES =
                    MethodHandles.lookup()
                            .findVarHandle(VarHandleNull.class, "refreshes", ConcurrentMap.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    boolean m(ConcurrentMap<Object, Object> pending) {
        return REFRESHES.compareAndSet(this, null, pending);
    }
}
