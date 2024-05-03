import org.checkerframework.checker.immutability.qual.Assignable;
import org.checkerframework.checker.immutability.qual.Immutable;

public class InvalidAssignability {
    final @Immutable Object io = null;
    // :: error: (initialization.field.uninitialized)
    @Immutable Object io2;
    // :: error: (initialization.field.uninitialized)
    @Assignable @Immutable Object io3;
    static final @Immutable Object io4 = null;
    // :: error: (initialization.static.field.uninitialized)
    static @Assignable @Immutable Object io5;
    // :: error: (one.assignability.invalid)
    final @Assignable @Immutable Object o = null;
    // :: error: (one.assignability.invalid)
    static final @Assignable @Immutable Object o2 = null;
}
