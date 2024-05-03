import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

@ReceiverDependantMutable
public class NotEveryInstFieldDefaultToRDM {
    // :: error: (assignment.type.incompatible)
    @ReceiverDependantMutable B b1 = new B();
    B b2 = new @ReceiverDependantMutable B();
    @Mutable C c = new @Mutable C();
    @Mutable D d = new @Mutable D();
    E e = new @Immutable E();
}

@ReceiverDependantMutable
class B {}

class C {}

@Mutable
class D {}

@Immutable
class E {}
