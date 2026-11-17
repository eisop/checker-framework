// The type of the enclosing instance of an inner class is not written in the inner class, so it is
// the type of "this" in the enclosing class, with the enclosing class's defaults.  A default of the
// inner class, or of a method or constructor of it, says nothing about the enclosing instance.
// Only a written annotation on the enclosing type of a receiver changes it.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

class InnerEnclosingDefaults<O> {
    O field;

    InnerEnclosingDefaults(O field) {
        this.field = field;
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    class Inner {
        // The default of Inner does not make the enclosing instance an
        // InnerEnclosingDefaults<@NonNull O>: nothing checks "o.new Inner()" against that.
        Inner() {
            // :: error: (dereference.of.nullable)
            field.toString();
        }

        Inner(int x) {
            // :: error: (dereference.of.nullable)
            InnerEnclosingDefaults.this.field.toString();
        }

        // A written constructor receiver is respected in the body, and -AcheckEnclosingExpr checks
        // it at "new"; see the nullness-enclosingexpr tests.
        Inner(InnerEnclosingDefaults<@NonNull O> InnerEnclosingDefaults.this, String s) {
            field.toString();
        }

        Inner(InnerEnclosingDefaults<@Nullable O> InnerEnclosingDefaults.this, long s) {
            // :: error: (dereference.of.nullable)
            field.toString();
        }

        void m() {
            // :: error: (dereference.of.nullable)
            field.toString();
        }

        // A receiver that does not annotate the enclosing type is the same as none.
        void plain(InnerEnclosingDefaults<O>.Inner this) {
            // :: error: (dereference.of.nullable)
            field.toString();
        }

        void written(InnerEnclosingDefaults<@NonNull O>.Inner this) {
            field.toString();
        }

        // A default on the method does not change the enclosing instance either.
        @DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
        void methodDefault() {
            // :: error: (dereference.of.nullable)
            field.toString();
        }
    }

    static void use() {
        InnerEnclosingDefaults<@Nullable String> o = new InnerEnclosingDefaults<>(null);
        // Each of these would throw a NullPointerException if the constructor were accepted.
        InnerEnclosingDefaults<@Nullable String>.Inner i = o.new Inner();
        o.new Inner(1);
        o.new Inner(2L);
        // A method whose receiver does not annotate the enclosing type can be called through any
        // enclosing instance.
        i.m();
        i.plain();
        i.methodDefault();
        // :: error: (method.invocation.invalid)
        i.written();

        InnerEnclosingDefaults<@NonNull String> p = new InnerEnclosingDefaults<>("");
        InnerEnclosingDefaults<@NonNull String>.Inner j = p.new Inner("");
        j.m();
        j.plain();
        j.written();
    }
}

// If the enclosing class has the default, the type of "this" in it has it too, so the inner class
// sees the enclosing instance as an InnerEnclosingDefaultsNonNull<@NonNull O>, and no instance
// with a nullable type argument can be created.
@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
class InnerEnclosingDefaultsNonNull<O extends @Nullable Object> {
    O field;

    InnerEnclosingDefaultsNonNull(O field) {
        this.field = field;
    }

    class Inner {
        Inner() {
            field.toString();
        }

        void m() {
            field.toString();
        }
    }

    static void use() {
        // :: error: (argument.type.incompatible)
        new InnerEnclosingDefaultsNonNull<@Nullable String>(null);
    }
}
