// Test case for eisop issue #2478:
// https://github.com/eisop/checker-framework/issues/2478
// The bounds of a type variable belong to its declaration. A default for UPPER_BOUND (or
// OTHERWISE) on a method or an inner class must not change the bound of an enclosing class's type
// variable inside that scope, while a method's own type parameter does get the method's default.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

class TypeVarBoundDeclarationDefaults<E> {
    E field;

    TypeVarBoundDeclarationDefaults(E field) {
        this.field = field;
    }

    E get() {
        return field;
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.UPPER_BOUND)
    void upperBound(E param, TypeVarBoundDeclarationDefaults<E> other) {
        // :: error: (dereference.of.nullable)
        this.field.toString();
        // :: error: (dereference.of.nullable)
        param.toString();
        // :: error: (dereference.of.nullable)
        other.field.toString();
        // :: error: (dereference.of.nullable)
        get().toString();
        E local = field;
        // :: error: (dereference.of.nullable)
        local.toString();
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.OTHERWISE)
    void otherwise(E param) {
        // :: error: (dereference.of.nullable)
        this.field.toString();
        // :: error: (dereference.of.nullable)
        param.toString();
    }

    // The bound of a method's own type parameter is declared in the method's scope, so it does get
    // the method's default.
    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.UPPER_BOUND)
    static <F> void ownTypeParameterUpperBound(F param) {
        param.toString();
        F local = param;
        local.toString();
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.OTHERWISE)
    static <F> void ownTypeParameterOtherwise(F param) {
        param.toString();
    }

    // A method's own type parameter next to a use of the class's type variable: each bound takes
    // the defaults of its own declaration.
    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.UPPER_BOUND)
    <F> void both(F ownParam, E classParam) {
        ownParam.toString();
        // :: error: (dereference.of.nullable)
        classParam.toString();
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.UPPER_BOUND)
    class Inner {
        void m(E param) {
            // :: error: (dereference.of.nullable)
            field.toString();
            // :: error: (dereference.of.nullable)
            param.toString();
        }

        // Declared in the inner class, whose default applies to its bound.
        <G> void own(G param) {
            param.toString();
        }
    }

    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.OTHERWISE)
    class InnerOtherwise {
        void m() {
            // :: error: (dereference.of.nullable)
            field.toString();
        }
    }

    // A default on a local class does not change the bound of the enclosing method's type
    // parameter.
    <T> void localClass(T t) {
        @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.UPPER_BOUND)
        class Local {
            void m(T param) {
                // :: error: (dereference.of.nullable)
                param.toString();
            }
        }
    }

    // A TYPE_VARIABLE_USE default still applies to the use itself.
    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.TYPE_VARIABLE_USE)
    void typeVariableUse(E param) {
        param.toString();
    }

    // Without a default in scope, the bound is @Nullable Object as always.
    void noDefault(E param) {
        // :: error: (dereference.of.nullable)
        this.field.toString();
        // :: error: (dereference.of.nullable)
        param.toString();
    }

    static void client() {
        TypeVarBoundDeclarationDefaults<@Nullable String> c =
                new TypeVarBoundDeclarationDefaults<>(null);
        // These calls are accepted, so the methods must not dereference the field or parameter.
        c.upperBound(null, c);
        c.otherwise(null);
        c.new Inner().m(null);
        c.new InnerOtherwise().m();
    }
}
