import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

import java.util.Date;

public class SuperClassTest {
    @ReceiverDependentMutable class SuperClass {
        @ReceiverDependentMutable Date p;

        @Immutable SuperClass(@Immutable Date p) {
            this.p = p;
        }

        void maliciouslyModifyDate(@Mutable SuperClass this) {
            p.setTime(2L);
        }
    }

    @Mutable class SubClass extends SuperClass {
        @Mutable SubClass() {
            // :: error: (super.invocation.invalid)
            super(new @Immutable Date(1L));
        }

        void test(String[] args) {
            @Mutable SubClass victim = new @Mutable SubClass();
            victim.maliciouslyModifyDate();
        }
    }

    @ReceiverDependentMutable class AnotherSubClass extends SuperClass {
        @ReceiverDependentMutable AnotherSubClass() {
            // :: error: (super.invocation.invalid)
            super(new @Immutable Date(1L));
        }

        void test(String[] args) {
            @Mutable SubClass victim = new @Mutable SubClass();
            victim.maliciouslyModifyDate();
        }
    }

    @ReceiverDependentMutable class Thief {
        @NotOnlyInitialized @ReceiverDependentMutable SuperClass2 victimCaptured;

        @ReceiverDependentMutable Thief(@UnderInitialization @ReceiverDependentMutable SuperClass2 victimCaptured) {
            this.victimCaptured = victimCaptured;
        }
    }

    @ReceiverDependentMutable class SuperClass2 {
        @ReceiverDependentMutable Date p;
        @NotOnlyInitialized @ReceiverDependentMutable Thief thief;

        @Mutable SuperClass2(@Mutable Date p) {
            this.p = p;
            // "this" escapes constructor and gets captured by thief
            this.thief = new @Mutable Thief(this);
        }
    }

    @Immutable class SubClass2 extends SuperClass2 {
        @Immutable SubClass2() {
            // This is not ok any more
            // :: error: (super.invocation.invalid)
            super(new @Mutable Date());
        }
    }

    @ReceiverDependentMutable class AnotherSubClass2 extends SuperClass2 {
        @ReceiverDependentMutable AnotherSubClass2() {
            // This is not ok any more
            // :: error: (super.invocation.invalid)
            super(new @Mutable Date());
        }
    }

    @ReceiverDependentMutable class SuperClass3 {
        @ReceiverDependentMutable Date p;

        @ReceiverDependentMutable SuperClass3(@ReceiverDependentMutable Date p) {
            this.p = p;
        }
    }

    @Mutable class SubClass3 extends SuperClass3 {
        @Mutable SubClass3() {
            super(new @Mutable Date(1L));
        }
    }

    @Immutable class AnotherSubClass3 extends SuperClass3 {
        @Immutable AnotherSubClass3() {
            super(new @Immutable Date(1L));
        }
    }

    @ReceiverDependentMutable class ThirdSubClass3 extends SuperClass3 {
        @ReceiverDependentMutable ThirdSubClass3() {
            super(new @ReceiverDependentMutable Date(1L));
        }
    }

    @ReceiverDependentMutable class SuperMethodInvocation {
        @ReceiverDependentMutable Object f;

        @ReceiverDependentMutable SuperMethodInvocation() {
            this.f = new @ReceiverDependentMutable Object();
        }

        void foo(@Mutable SuperMethodInvocation this) {
            this.f = new @Mutable Object();
        }
    }

    @Immutable class Subclass extends SuperMethodInvocation {

        @Immutable Subclass() {
            // TODO Still need to investigate if it's proper to allow such reassignment
            // We may as well say "f is alreayd initializaed" so f can't be reassigned.
            // The way to implement it is to check @UnderInitialization(SuperMethodInvocation.class)
            // and f is within the class hierarchy range Object.class ~ SuperMethodInvocation.class,
            // so forbid reassigning it.
            this.f = new @Immutable Object();
        }

        // Yes, the overriding method can be contravariant(going to supertype) in terms of
        // receiver and formal parameters. This ensures that all the existing method invocation
        // won't break just because maybe some days later, the method is overriden in the
        // subclass :)
        @Override
        void foo(@Readonly Subclass this) {
            // But this super method invocation definitely shouldn't typecheck. "super" has the same
            // mutability as the declared "this" parameter. Because the declared receiver can now
            // be passed in @Immutable objects, if we allowed this super invocation, then its
            // abstract
            // state will be changed and immutability guarantee will be compromised. So, we still
            // retain the standard/default typechecking rules for calling super method using "super"
            // :: error: (method.invocation.invalid)
            super.foo();
        }

        void test(String[] args) {
            // Example that illustrates the point above is here: calling foo() method will alter the
            // abstract state of sub object, which should be @Immutable
            @Immutable Subclass sub = new @Immutable Subclass();
            sub.foo();
        }
    }
}
