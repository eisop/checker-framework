import org.checkerframework.checker.immutability.qual.Assignable;
import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;

@Immutable
class A<T extends @Readonly Object> {
    @Assignable T t;

    @Immutable
    A(T t) {
        this.t = t;
    }
}

@Immutable
class ImmutableClass3 extends A<@Mutable Object> {
    @Immutable
    ImmutableClass3() {
        super(new @Mutable Object());
    }

    void foo(@Immutable ImmutableClass3 this) {
        /*This is acceptable. t is not in the abstract state of
        the entire object because T has upper bound @Readonly*/
        @Mutable Object mo = this.t;
        // Be default, we can't assign to t; But with the assignability dimension,
        // we can do that now by annotating @Assignable to t
        this.t = new @Mutable Object();
    }
}
