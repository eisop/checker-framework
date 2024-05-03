import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

import java.util.Date;

@ReceiverDependantMutable
class Thief {
    @NotOnlyInitialized @ReceiverDependantMutable SuperClass2 victimCaptured;

    @ReceiverDependantMutable
    Thief(@UnderInitialization @ReceiverDependantMutable SuperClass2 victimCaptured) {
        this.victimCaptured = victimCaptured;
    }
}

@ReceiverDependantMutable
public class SuperClass2 {
    @ReceiverDependantMutable Date p;
    @NotOnlyInitialized @ReceiverDependantMutable Thief thief;

    @Mutable
    SuperClass2(@Mutable Date p) {
        this.p = p;
        // "this" escapes constructor and gets captured by thief
        this.thief = new @Mutable Thief(this);
    }
}

@Immutable
class SubClass2 extends SuperClass2 {
    @Immutable
    SubClass2() {
        // This is not ok any more
        // :: error: (super.invocation.invalid)
        super(new @Mutable Date());
    }
}

@ReceiverDependantMutable
class AnotherSubClass2 extends SuperClass2 {
    @ReceiverDependantMutable
    AnotherSubClass2() {
        // This is not ok any more
        // :: error: (super.invocation.invalid)
        super(new @Mutable Date());
    }
}
