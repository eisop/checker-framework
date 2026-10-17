// Test case for eisop#2288:
// https://github.com/eisop/checker-framework/issues/2288
// In a field initializer or initializer block, the receiver used to viewpoint-adapt a field must
// be a use of the class, not its declaration. Otherwise, a field type that mentions a class type
// variable (the field being read, or the field being initialized) gets the type-variable
// declaration, and inference crashes when it substitutes that declaration into the bound of U.
public class Issue2288<T> {
    static <U extends V, V> V[] make(U[] u) {
        return null;
    }

    static <U extends V, V> V[] make() {
        return null;
    }

    T[] a = null;

    T[] b = make(a);

    Object c = make(this.a);

    // The class type variable reaches inference only through the field's type.
    T[] g = make();

    {
        T[] d = make(a);
    }

    Issue2288() {
        T[] e = make(a);
    }

    void method() {
        T[] f = make(a);
    }
}
