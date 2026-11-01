// An override whose receiver is the type of "this" is never a narrowing, even when a default puts a
// qualifier on every use of a type variable, as the JSpecify reference checker does in unmarked
// code.  The type of "this" is the class applied to its own type parameters, with the class's
// defaults.  These overrides were reported as override.receiver.invalid.
//
// A default that only the overriding method is in the scope of is not part of the type of "this",
// so a receiver that it changes is still a narrowing: through a Parent<@Nullable String>, a caller
// could pass a receiver whose field is null to a body that assumes it is non-null.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
public class ReceiverDefaultedTypeVarUse<E> {
    // Implicit receiver.
    @Override
    public String toString() {
        return "";
    }

    // Explicit receiver that writes no annotation.
    @Override
    public int hashCode(ReceiverDefaultedTypeVarUse<E> this) {
        return 0;
    }

    // Explicit receiver that writes the qualifier the class default gives.
    @Override
    public boolean equals(ReceiverDefaultedTypeVarUse<@Nullable E> this, @Nullable Object o) {
        return false;
    }

    // Explicit receiver of an inner class that does not spell out the enclosing type.
    class Inner {
        @Override
        public int hashCode(Inner this) {
            return 0;
        }
    }
}

class ReceiverDefaultedTypeVarUseParent<T> {
    void implicitReceiver() {}

    void explicitReceiver() {}

    void upperBound() {}

    void written() {}
}

class ReceiverDefaultedTypeVarUseChild<E> extends ReceiverDefaultedTypeVarUseParent<E> {
    E field;

    ReceiverDefaultedTypeVarUseChild(E field) {
        this.field = field;
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    @Override
    // :: error: (override.receiver.invalid)
    void implicitReceiver() {
        this.field.toString();
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    @Override
    // :: error: (override.receiver.invalid)
    void explicitReceiver(ReceiverDefaultedTypeVarUseChild<E> this) {
        this.field.toString();
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.UPPER_BOUND)
    @Override
    // :: error: (override.receiver.invalid)
    void upperBound() {
        this.field.toString();
    }

    @Override
    // :: error: (override.receiver.invalid)
    void written(ReceiverDefaultedTypeVarUseChild<@NonNull E> this) {
        this.field.toString();
    }

    static void use() {
        ReceiverDefaultedTypeVarUseParent<@Nullable String> p =
                new ReceiverDefaultedTypeVarUseChild<@Nullable String>(null);
        // Each of these calls throws a NullPointerException.
        p.implicitReceiver();
        p.explicitReceiver();
        p.upperBound();
        p.written();
    }
}

class ReceiverDefaultedTypeVarUseOuter<O> {
    O field;

    ReceiverDefaultedTypeVarUseOuter(O field) {
        this.field = field;
    }

    class Inner extends ReceiverDefaultedTypeVarUseParent<String> {
        @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
        @Override
        // :: error: (override.receiver.invalid)
        void implicitReceiver() {
            field.toString();
        }

        @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
        @Override
        // :: error: (override.receiver.invalid)
        void explicitReceiver(Inner this) {
            field.toString();
        }

        @Override
        void written(ReceiverDefaultedTypeVarUseOuter<O>.Inner this) {}
    }
}

// The bound makes every use of E non-null, so writing @NonNull on it is no narrowing.
class ReceiverDefaultedTypeVarUseBounded<E extends @NonNull Object>
        extends ReceiverDefaultedTypeVarUseParent<E> {
    @Override
    void written(ReceiverDefaultedTypeVarUseBounded<@NonNull E> this) {}
}
