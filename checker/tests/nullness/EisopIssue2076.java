// Test case for https://github.com/eisop/checker-framework/issues/2076 :
// an override whose receiver narrows the type arguments of the overridden method's receiver
// was not reported.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

    static void use() {
        Child<@Nullable String> child = new Child<>(null);
        Parent<@Nullable String> parent = child;
        parent.inspect();
    }
}
