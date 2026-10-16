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

    static void use() {
        Child<@Nullable String> child = new Child<>(null);
        Parent<@Nullable String> parent = child;
        parent.inspect();
    }
}
