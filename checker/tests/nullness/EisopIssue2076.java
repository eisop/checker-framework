// Test case for https://github.com/eisop/checker-framework/issues/2076 :
// an override whose receiver narrows the type arguments of the overridden method's receiver
// was not reported.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class EisopIssue2076 {
    static class Parent<E extends @Nullable Object> {
        E value;

        Parent(E value) {
            this.value = value;
        }

        void inspect(Parent<E> this) {}

        void implicitReceiver() {}

        void nullableArg(Parent<E> this) {}

        void nonNullArg(Parent<@NonNull E> this) {}
    }

    static class Child<E extends @Nullable Object> extends Parent<E> {
        Child(E value) {
            super(value);
        }

        @Override
        // :: error: (override.receiver.invalid)
        void inspect(Child<@NonNull E> this) {
            value.toString();
        }

        @Override
        void implicitReceiver(Child<E> this) {}

        @Override
        // :: error: (override.receiver.invalid)
        void nullableArg(Child<@Nullable E> this) {}

        @Override
        void nonNullArg(Child<@NonNull E> this) {}
    }

    static class Fixed extends Parent<@Nullable String> {
        Fixed() {
            super(null);
        }

        @Override
        void inspect(Fixed this) {}
    }

    // The type arguments of the enclosing type count, too.
    static class Outer<O extends @Nullable Object> {
        class Inner<I extends @Nullable Object> {
            void m(Outer<O>.Inner<I> this) {}
        }

        class SubInner<I extends @Nullable Object> extends Inner<I> {
            @Override
            void m(Outer<O>.SubInner<I> this) {}
        }

        class NarrowedSubInner<I extends @Nullable Object> extends Inner<I> {
            @Override
            // :: error: (override.receiver.invalid)
            void m(Outer<@NonNull O>.NarrowedSubInner<I> this) {}
        }
    }

    // A type parameter of the overrider's class that does not flow into the overridden class is
    // fixed by nothing, so narrowing it is reported.
    static class ParentOne<E extends @Nullable Object> {
        void m(ParentOne<E> this) {}
    }

    static class ChildTwo<E extends @Nullable Object, F extends @Nullable Object>
            extends ParentOne<E> {
        F f;

        ChildTwo(F f) {
            this.f = f;
        }

        @Override
        // :: error: (override.receiver.invalid)
        void m(ChildTwo<E, @NonNull F> this) {
            f.toString();
        }
    }

    static class ChildTwoOk<E extends @Nullable Object, F extends @Nullable Object>
            extends ParentOne<E> {
        @Override
        void m() {}
    }

    // The type arguments of enclosing types are compared, too.
    static class Outer2<O extends @Nullable Object> {
        class Inner {
            void m(Outer2<O>.Inner this) {}
        }

        class Sub extends Inner {
            @Override
            // :: error: (override.receiver.invalid)
            void m(Outer2<@NonNull O>.Sub this) {}
        }

        class SubOk extends Inner {
            @Override
            void m(Outer2<O>.SubOk this) {}
        }
    }

    // An enclosing type argument that the overridden receiver fixes to the same type is accepted.
    static class Outer3<O extends @Nullable Object> {
        class Inner {
            void m(Outer3<@NonNull O>.Inner this) {}
        }

        class SameSub extends Inner {
            @Override
            void m(Outer3<@NonNull O>.SameSub this) {}
        }

        class DifferentSub extends Inner {
            @Override
            // :: error: (override.receiver.invalid)
            void m(Outer3<@Nullable O>.DifferentSub this) {}
        }
    }

    // A raw supertype fixes no type argument, so narrowing E, which does not flow into Parent, is
    // reported, as for a type parameter that the overridden class never sees.
    @SuppressWarnings({"rawtypes", "unchecked"})
    static class RawChild<E extends @Nullable Object> extends Parent {
        RawChild() {
            super(null);
        }

        @Override
        // :: error: (override.receiver.invalid)
        void inspect(RawChild<@NonNull E> this) {}
    }

    // A polymorphic qualifier gets no special treatment: a poly type argument in the overrider's
    // receiver must equal the overridden receiver's type argument.
    static class PolyParent<E extends @Nullable Object> {
        @PolyNull Object m(PolyParent<E> this, @PolyNull Object p) {
            return p;
        }

        @SuppressWarnings("return")
        @PolyNull Object k(PolyParent<@PolyNull E> this) {
            return null;
        }
    }

    static class PolyChild<E extends @Nullable Object> extends PolyParent<E> {
        E value;

        PolyChild(E value) {
            this.value = value;
        }

        @Override
        @SuppressWarnings("return")
        // :: error: (override.receiver.invalid)
        @PolyNull Object m(PolyChild<@PolyNull E> this, @PolyNull Object p) {
            return value;
        }

        @Override
        @SuppressWarnings("return")
        @PolyNull Object k(PolyChild<@PolyNull E> this) {
            return value;
        }
    }

    // An F-bounded class parameter.
    static class FParent<E extends Comparable<E>> {
        void m(FParent<E> this) {}
    }

    static class FChild<E extends Comparable<E>> extends FParent<E> {
        @Override
        void m(FChild<E> this) {}
    }

    // The overridden class is an interface.
    interface Iface<E extends @Nullable Object> {
        default void m(Iface<E> this) {}
    }

    static class Impl<E extends @Nullable Object> implements Iface<E> {
        @Override
        public void m(Impl<E> this) {}
    }

    static class ImplNarrowed<E extends @Nullable Object> implements Iface<E> {
        @Override
        // :: error: (override.receiver.invalid)
        public void m(ImplNarrowed<@NonNull E> this) {}
    }

    // A generic class that extends a class without type parameters.
    static class Base {
        void m(Base this) {}
    }

    static class GenericChild<E extends @Nullable Object> extends Base {
        @Override
        void m(GenericChild<E> this) {}
    }

    static class GenericChildNarrowed<E extends @Nullable Object> extends Base {
        @Override
        // :: error: (override.receiver.invalid)
        void m(GenericChildNarrowed<@NonNull E> this) {}
    }

    // E is universal and F matches, so the override is valid even though the receivers are not
    // equal as a whole.
    static class TwoParent<E extends @Nullable Object, F extends @Nullable Object> {
        void m(TwoParent<@PolyNull E, @NonNull F> this) {}
    }

    static class TwoChild<E extends @Nullable Object, F extends @Nullable Object>
            extends TwoParent<E, F> {
        @Override
        void m(TwoChild<E, @NonNull F> this) {}
    }

    static void use() {
        Child<@Nullable String> child = new Child<>(null);
        Parent<@Nullable String> parent = child;
        parent.inspect();
    }
}
