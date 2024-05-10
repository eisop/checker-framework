import org.checkerframework.checker.immutability.qual.Assignable;
import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

import java.util.Date;

@ReceiverDependantMutable
public class AssignableExample {
    @Immutable Object o;
    @Immutable Date date;
    @Assignable @Immutable Date assignableDate;

    // :: error: (initialization.fields.uninitialized)
    @Mutable AssignableExample() {
        o = new @Immutable Object();
    }

    void foo(@Immutable AssignableExample this) {
        // :: error: (illegal.field.write)
        this.date = new @Immutable Date();
        this.assignableDate = new @Immutable Date();
    }

    void foo2(@Mutable AssignableExample this) {
        this.o = new @Immutable Object();
    }
}

// :: error: (super.invocation.invalid)
@ReceiverDependantMutable class Subclass extends AssignableExample {
    void bar(@Immutable Subclass this) {
        // :: error: (illegal.field.write)
        this.date = new @Immutable Date();
        this.assignableDate = new @Immutable Date();
    }

    void bar2(@Mutable Subclass this) {
        this.date = new @Immutable Date();
        this.assignableDate = new @Immutable Date();
    }
}
