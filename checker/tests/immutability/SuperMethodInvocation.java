import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@ReceiverDependantMutable
public class SuperMethodInvocation {
    @ReceiverDependantMutable Object f;

    @ReceiverDependantMutable
    SuperMethodInvocation() {
        this.f = new @ReceiverDependantMutable Object();
    }

    void foo(@Mutable SuperMethodInvocation this) {
        this.f = new @Mutable Object();
    }
}

@Immutable
class Subclass extends SuperMethodInvocation {

    @Immutable
    Subclass() {
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
        // be passed in @Immutable objects, if we allowed this super invocation, then its abstract
        // state will be changed and immutability guarantee will be compromised. So, we still
        // retain the standard/default typechecking rules for calling super method using "super"
        // :: error: (method.invocation.invalid)
        super.foo();
    }

    public static void main(String[] args) {
        // Example that illustrates the point above is here: calling foo() method will alter the
        // abstract state of sub object, which should be @Immutable
        @Immutable Subclass sub = new @Immutable Subclass();
        sub.foo();
    }
}
