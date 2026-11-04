// Only an instance of the overrider's class can run an override.  So an override whose receiver is
// the type of "this" is not a narrowing, even when a default puts a qualifier on every use of a
// type variable, as the JSpecify reference checker does in unmarked code: the type of "this" is the
// class applied to its own type parameters with the class's defaults, and the same defaults apply
// to the class's extends clause, constructors and fields, so it accepts every instance of the
// class.  Its enclosing type is the type of "this" in the enclosing class, with that class's
// defaults.  These overrides were reported as override.receiver.invalid.
//
// A default that only the overriding method is in the scope of is not part of the type of "this",
// so a receiver that it changes is still a narrowing: through a Parent<@Nullable String>, a caller
// could pass a receiver whose field is null to a body that assumes it is non-null.
//
// The type of the enclosing instance is not written in an inner class, so a default of the inner
// class or of one of its methods does not change it; it stays the type of "this" in the enclosing
// class, and a dereference of a nullable field of the enclosing instance is reported in the body.
// See InnerEnclosingDefaults.java.

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

    void writtenNonNull() {}
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

    // An UPPER_BOUND default does not change the bound of the class's type variable E, so the
    // receiver is not narrowed, and the dereference is reported instead.
    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.UPPER_BOUND)
    @Override
    void upperBound() {
        // :: error: (dereference.of.nullable)
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

    // A default on the overriding method does not change the enclosing type of its receiver, which
    // stays the type of "this" in the enclosing class, so the override is no narrowing.
    class Inner extends ReceiverDefaultedTypeVarUseParent<String> {
        @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
        @Override
        void implicitReceiver() {
            // :: error: (dereference.of.nullable)
            field.toString();
        }

        @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
        @Override
        void explicitReceiver(Inner this) {
            // :: error: (dereference.of.nullable)
            field.toString();
        }

        @Override
        void written(ReceiverDefaultedTypeVarUseOuter<O>.Inner this) {}

        // A written annotation on the enclosing type is a narrowing.
        @Override
        // :: error: (override.receiver.invalid)
        void writtenNonNull(ReceiverDefaultedTypeVarUseOuter<@NonNull O>.Inner this) {
            field.toString();
        }
    }
}

// The bound makes every use of E non-null, so writing @NonNull on it is no narrowing.
class ReceiverDefaultedTypeVarUseBounded<E extends @NonNull Object>
        extends ReceiverDefaultedTypeVarUseParent<E> {
    @Override
    void written(ReceiverDefaultedTypeVarUseBounded<@NonNull E> this) {}
}

// A default of an inner class is not part of the type of its enclosing instance: through a
// ReceiverDefaultedTypeVarUseParent<String>, a caller could pass an Inner whose enclosing instance
// has a null field.  The receiver's enclosing type stays the type of "this" in the enclosing class,
// so the dereference is reported in the body and the override is no narrowing.
class ReceiverDefaultedTypeVarUseInnerDefault<O> {
    O field;

    ReceiverDefaultedTypeVarUseInnerDefault(O field) {
        this.field = field;
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    class Inner extends ReceiverDefaultedTypeVarUseParent<String> {
        @Override
        void implicitReceiver() {
            // :: error: (dereference.of.nullable)
            field.toString();
        }
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    class Mid<M> {
        class Inner<I> {
            @Override
            public String toString() {
                // :: error: (dereference.of.nullable)
                return field.toString();
            }
        }
    }

    static void use() {
        ReceiverDefaultedTypeVarUseInnerDefault<@Nullable String> o =
                new ReceiverDefaultedTypeVarUseInnerDefault<>(null);
        ReceiverDefaultedTypeVarUseParent<String> p = o.new Inner();
        // Each of these calls would throw a NullPointerException if the bodies were accepted.
        p.implicitReceiver();
        Object x = o.new Mid<String>().new Inner<String>();
        x.toString();
    }
}

// A default of the class is part of the type of "this", but also of its extends clause and its
// constructors, so no instance of the class can be seen as a
// ReceiverDefaultedTypeVarUseParent<@Nullable String>, and the override is not a narrowing.  This
// also holds for F, which the overridden class does not see.
@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
class ReceiverDefaultedTypeVarUseNonNullClass<
                E extends @Nullable Object, F extends @Nullable Object>
        extends ReceiverDefaultedTypeVarUseParent<E> {
    E field;
    F other;

    ReceiverDefaultedTypeVarUseNonNullClass(E field, F other) {
        this.field = field;
        this.other = other;
    }

    @Override
    void implicitReceiver() {
        field.toString();
        other.toString();
    }

    static void use() {
        ReceiverDefaultedTypeVarUseParent<@Nullable String> p =
                // :: error: (assignment.type.incompatible)
                new ReceiverDefaultedTypeVarUseNonNullClass<@Nullable String, @Nullable String>(
                        "", "");
        // :: error: (argument.type.incompatible)
        new ReceiverDefaultedTypeVarUseNonNullClass<@Nullable String, @Nullable String>(null, "");
    }
}

// A default of a local class is not part of the type of its enclosing instance either.
class ReceiverDefaultedTypeVarUseLocal<O> {
    O field;

    ReceiverDefaultedTypeVarUseLocal(O field) {
        this.field = field;
    }

    ReceiverDefaultedTypeVarUseParent<String> make() {
        @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
        class Local extends ReceiverDefaultedTypeVarUseParent<String> {
            @Override
            void implicitReceiver() {
                // :: error: (dereference.of.nullable)
                field.toString();
            }
        }
        return new Local();
    }
}
