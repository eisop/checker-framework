import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.PolyMutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@ReceiverDependantMutable
class B {
    @PolyMutable
    B getObject() {
        return null;
    }

    @PolyMutable
    B getSecondObject(@PolyMutable B this) {
        return null;
    }

    @PolyMutable
    B getThirdObject(@Mutable B this) {
        return null;
    }

    @Immutable
    B getForthObject() {
        return this.getThirdObject();
    }
}

public class Polymorphism {
    void test1(@Mutable B mb) {
        @Mutable Object l = mb.getObject();
        @Immutable Object r = mb.getObject();
    }

    void test2(@Mutable B mb) {
        @Mutable Object l = mb.getSecondObject();
        // TODO Should be poly.invocation.error something...
        // :: error: (assignment.type.incompatible)
        @Immutable Object r = mb.getSecondObject();
    }

    void test3(@Immutable B imb) {
        // TODO Should be poly.invocation.error something...
        // :: error: (assignment.type.incompatible)
        @Mutable Object l = imb.getSecondObject();
        @Immutable Object r = imb.getSecondObject();
    }

    void test4(@Mutable B b) {
        // This correctly typechecks
        @Immutable Object r = b.getObject().getThirdObject();
    }

    // TODO Poly return type used on poly receiver. This is not yet implemented yet in CF
    void test5(@Mutable B b) {
        // TODO Should typecheck.
        // :: error: (assignment.type.incompatible)
        @Immutable Object r = b.getSecondObject().getSecondObject();
    }
}
