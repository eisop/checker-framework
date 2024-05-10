import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.PolyMutable;
import org.checkerframework.checker.immutability.qual.Readonly;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

// :: error: (class.bound.invalid)
@Readonly public class InvalidBound {}

// :: error: (class.bound.invalid)
@PolyMutable class A {}

// ok
@Immutable
class C {
    @Immutable
    C() {}
}

// ok
@Mutable
class D {}

// ok
@ReceiverDependantMutable
class E {}
