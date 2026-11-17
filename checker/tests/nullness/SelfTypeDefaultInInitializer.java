// In a field initializer or an initializer block, the type of "this" has the class's defaults, as
// in a method.  An implicit receiver there was the class without its defaults, so a call to a
// method of the class was reported as method.invocation.invalid.

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
class SelfTypeDefaultInInitializer<E> {
    int m(@UnknownInitialization SelfTypeDefaultInInitializer<E> this) {
        return 0;
    }

    int implicit = m();
    int explicit = this.m();
    int block;

    {
        block = m();
    }
}

@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
class SelfTypeDefaultInInitializerNonNull<E extends @Nullable Object> {
    int m(@UnknownInitialization SelfTypeDefaultInInitializerNonNull<E> this) {
        return 0;
    }

    int implicit = m();
    int explicit = this.m();
}

// A default of an inner class is not part of the type of its enclosing instance.
class SelfTypeDefaultInInitializerOuter<O> {
    O field;

    SelfTypeDefaultInInitializerOuter(O field) {
        this.field = field;
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    class Inner {
        // :: error: (dereference.of.nullable)
        String s = field.toString();
    }

    static void use() {
        // Throws a NullPointerException.
        new SelfTypeDefaultInInitializerOuter<@Nullable String>(null).new Inner();
    }
}
