// Test case for https://github.com/eisop/checker-framework/issues/2461
// Checking an anonymous subclass of an inner class that is instantiated with an enclosing
// instance crashed with a NullPointerException ("thisEnclosing is null") in
// AnnotatedTypeMirror.AnnotatedDeclaredType.getErased: the erased underlying type of the
// inner class still has an enclosing type, but the annotated type of the anonymous class has
// no annotated enclosing type, so the two enclosing-type chains have different lengths.

public class EisopIssue2461<K> {
    class It<T> {
        T t;

        It(T t) {
            this.t = t;
        }

        T get() {
            return t;
        }
    }

    class Keys {
        // Explicit enclosing qualifier on a diamond anonymous subclass of the inner class.
        void explicitQualifierAssign(Object n, EisopIssue2461<K> outer) {
            It<?> x = outer.new It<>(n) {};
            x.toString();
        }

        void explicitQualifierField(Object n, EisopIssue2461<K> outer) {
            Object x = outer.new It<>(n) {}.t;
            x.toString();
        }

        void explicitQualifierMethod(Object n, EisopIssue2461<K> outer) {
            Object x = outer.new It<>(n) {}.get();
            x.toString();
        }

        // Explicit (non-diamond) type argument on an anonymous subclass.
        void explicitTypeArg(Object n, EisopIssue2461<K> outer) {
            Object x = outer.new It<Object>(n) {}.t;
            x.toString();
        }
    }

    static class Top {
        class It<T> {
            T t;

            It(T t) {
                this.t = t;
            }
        }
    }

    // new Top().new It<>(...) {} : enclosing instance is a fresh Top, not the current receiver.
    void nestedNew(Object n) {
        Object x = new Top().new It<>(n) {}.t;
        x.toString();
    }
}
