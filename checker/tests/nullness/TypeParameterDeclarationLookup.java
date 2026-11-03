// Defaulting finds each type parameter's declaration to tell an explicit bound from none: the
// upper bound of `<T extends Object>` is non-null, and that of `<T>` is nullable. Each case
// declares a type parameter somewhere its declaration is harder to find.

import org.checkerframework.checker.nullness.qual.Nullable;

class TypeParameterDeclarationLookup<T, U extends Object> {
    void classTypeParameters(T t, U u) {
        // :: error: (dereference.of.nullable)
        t.toString();
        u.toString();
    }

    // Method type parameters with the names, but not the bounds, of the class type parameters.
    <T extends Object, U> void shadowing(T t, U u) {
        t.toString();
        // :: error: (dereference.of.nullable)
        u.toString();
    }

    <V extends Object, W> TypeParameterDeclarationLookup(V v, W w) {
        v.toString();
        // :: error: (dereference.of.nullable)
        w.toString();
    }

    <A extends Object, B extends A, C extends Object & Comparable<C>> void boundedByOthers(
            B b, C c) {
        b.toString();
        c.toString();
    }

    // An inner class whose type parameter shadows the outer one; U is the outer type parameter.
    class Inner<T extends Object> {
        void m(T t, U u) {
            t.toString();
            u.toString();
        }
    }

    void localAndAnonymousClasses() {
        class Local<L extends Object, M> {
            void m(L l, M m) {
                l.toString();
                // :: error: (dereference.of.nullable)
                m.toString();
            }
        }
        new Object() {
            <P extends Object, Q> void m(P p, Q q) {
                p.toString();
                // :: error: (dereference.of.nullable)
                q.toString();
            }
        };
    }

    <R extends Object, S> void inLambda(R r, S s) {
        Runnable run =
                () -> {
                    r.toString();
                    // :: error: (dereference.of.nullable)
                    s.toString();
                };
    }

    // Type parameters declared in a class that is not on the path being checked, and that comes
    // later in the file.
    void declaredElsewhere() {
        // :: error: (type.arguments.not.inferred)
        TypeParameterDeclarationLookupLater.bounded(null);
        TypeParameterDeclarationLookupLater.unbounded(null);
        // :: error: (type.argument.type.incompatible)
        new TypeParameterDeclarationLookupLater<@Nullable String>();
    }
}

class TypeParameterDeclarationLookupLater<E extends Object> {
    static <F extends Object> F bounded(F f) {
        return f;
    }

    static <G> G unbounded(G g) {
        return g;
    }
}
