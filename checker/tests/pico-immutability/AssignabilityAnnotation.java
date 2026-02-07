import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;

public class AssignabilityAnnotation {
    int a;
    @Assignable int b;
    final int c;
    // :: error: (assignability.declaration.invalid)
    final @Assignable int d;
    final @Immutable Object io = null;
    @Immutable Object io2;
    @Assignable @Immutable Object io3;
    static final @Immutable Object io4 = null;
    static @Assignable @Immutable Object io5;
    // :: error: (assignability.declaration.invalid)
    final @Assignable @Immutable Object o = null;
    // :: error: (assignability.declaration.invalid)
    static final @Assignable @Immutable Object o2 = null;

    // Fields are initialized here to prevent initailization errors
    AssignabilityAnnotation() {
        a = 1;
        c = 1;
        d = 1;
        io2 = new Object();
    }
}
