import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class ImmutableClass {

    @Readonly Object readonlyField;
    @ReceiverDependentMutable Object rdmField;
    @Immutable Object immutableField;

    @Immutable ImmutableClass(@Immutable Object immutableObject) {
        this.readonlyField = immutableObject;
        this.rdmField = immutableObject;
        this.immutableField = immutableObject;
    }

    // ok
    @Immutable class ImmutableClass2<T extends @Readonly Object> {
        @Immutable ImmutableClass2() {}
    }

    // ok
    @Immutable class ImmutableClass3<T extends @ReceiverDependentMutable Object> {
        @Immutable ImmutableClass3() {}
    }

    // ok
    @Immutable class ImmutableClass4<T extends @Mutable Object> {
        @Immutable ImmutableClass4() {}
    }

    // ok
    @Immutable class ImmutableClass5<T extends @Mutable Object, S extends T> {
        @Immutable ImmutableClass5() {}
    }

    @Immutable class ImmutableClass6<T extends @Immutable Object> {
        @Immutable ImmutableClass6() {}
    }

    @Immutable class ImmutableClass7 {
        @Immutable ImmutableClass7() {}

        // Should NOT have warnings for type parameter with non-immutable upper bound
        // if the type parameter is declared on generic method(?)
        <S extends @Mutable Object> S foo(@Immutable ImmutableClass7 this) {
            return null;
        }
    }

    @Immutable class A<T extends @Readonly Object> {
        @Assignable T t;

        @Immutable A(T t) {
            this.t = t;
        }
    }

    @Immutable class ImmutableClass8 extends A<@Mutable Object> {
        @Immutable ImmutableClass8() {
            super(new @Mutable Object());
        }

        void foo(@Immutable ImmutableClass8 this) {
            /*This is acceptable. t is not in the abstract state of
            the entire object because T has upper bound @Readonly*/
            @Mutable Object mo = this.t;
            // Be default, we can't assign to t; But with the assignability dimension,
            // we can do that now by annotating @Assignable to t
            this.t = new @Mutable Object();
        }
    }
}
