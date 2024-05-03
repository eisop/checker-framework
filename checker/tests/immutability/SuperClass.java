import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

import java.util.Date;

@ReceiverDependantMutable
public class SuperClass {
    @ReceiverDependantMutable Date p;

    @Immutable
    SuperClass(@Immutable Date p) {
        this.p = p;
    }

    void maliciouslyModifyDate(@Mutable SuperClass this) {
        p.setTime(2L);
    }
}

class SubClass extends SuperClass {
    @Mutable
    SubClass() {
        // :: error: (super.invocation.invalid)
        super(new @Immutable Date(1L));
    }

    public static void main(String[] args) {
        @Mutable SubClass victim = new @Mutable SubClass();
        victim.maliciouslyModifyDate();
    }
}

@ReceiverDependantMutable
class AnotherSubClass extends SuperClass {
    @ReceiverDependantMutable
    AnotherSubClass() {
        // :: error: (super.invocation.invalid)
        super(new @Immutable Date(1L));
    }

    public static void main(String[] args) {
        @Mutable SubClass victim = new @Mutable SubClass();
        victim.maliciouslyModifyDate();
    }
}
