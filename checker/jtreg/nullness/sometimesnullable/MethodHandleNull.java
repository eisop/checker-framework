/*
 * @test
 *
 * @summary MethodHandle's invocation methods are signature-polymorphic: whether an argument may be
 * null depends on the target's parameter type, so the annotated JDK conservatively forbids null.
 * -Astubs=sometimes-nullable.astub opts in to the unsound-but-convenient reading.
 *
 * @compile/fail/ref=MethodHandleNull.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker MethodHandleNull.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=sometimes-nullable.astub MethodHandleNull.java
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class MethodHandleNull {
    static final MethodHandle HANDLE;

    static {
        try {
            HANDLE =
                    MethodHandles.lookup()
                            .findStatic(
                                    MethodHandleNull.class,
                                    "target",
                                    MethodType.methodType(int.class, String.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static int target(String s) {
        return s == null ? 0 : s.length();
    }

    void passNull() throws Throwable {
        HANDLE.invoke((Object) null);
        HANDLE.invokeExact((Object) null);
        HANDLE.invokeWithArguments((Object) null);
        HANDLE.bindTo(null);
    }
}
