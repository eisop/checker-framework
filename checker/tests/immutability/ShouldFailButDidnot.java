import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;

class ShouldFailButDidnot {
    void foo() {
        @Readonly Object o = new @Immutable Object();
        o = new @Mutable Object();
    }
}
