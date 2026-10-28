// Test case for eisop#2288: in a field initializer or initializer block, viewpoint adaptation must
// use a use of the class, not its declaration. Otherwise, a field type that mentions a class type
// variable put the type-variable declaration into the bound of U and crashed inference.

import viewpointtest.quals.*;

public class Issue2288 {
    static <U extends V, V> V[] make(U[] u) {
        return null;
    }

    static <U extends V, V> V[] make() {
        return null;
    }

    static class Plain<T> {
        T[] a = null;

        T[] b = make(a);

        Object c = make(this.a);

        // The class type variable reaches inference only through the field's type.
        T[] g = make();

        {
            T[] d = make(a);
        }

        Plain() {
            T[] e = make(a);
        }

        void method() {
            T[] f = make(a);
        }
    }

    // The receiver is still adapted to the class's own qualifier.
    @SuppressWarnings({
        "inconsistent.constructor.type",
        "super.invocation.invalid",
        "cast.unsafe.constructor.invocation"
    })
    @A static class Adapted<T> {
        T[] t = make();

        @ReceiverDependentQual Object compatible = new @A Object();

        // :: error: (assignment.type.incompatible)
        @ReceiverDependentQual Object incompatible = new @B Object();
    }
}
