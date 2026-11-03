// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2461
// Checking an anonymous subclass of an inner class that is instantiated with an enclosing
// instance crashed with a NullPointerException ("thisEnclosing is null") in
// AnnotatedTypeMirror.AnnotatedDeclaredType.getErased: the enclosing type of the annotated type
// of the anonymous class is the type of the enclosing instance, but javac's is the class that
// lexically encloses the anonymous class, and that chain was longer.

import org.checkerframework.checker.nullness.qual.Nullable;

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

    static class Outer<E> {
        class In {
            E e;

            In(E e) {
                this.e = e;
            }
        }
    }

    // The type argument of the enclosing instance reaches the anonymous class's superclass.
    class Uses {
        void nullable(Outer<@Nullable String> o) {
            // :: error: (dereference.of.nullable)
            o.new In(null) {}.e.toString();
        }

        void nonNull(Outer<String> o) {
            o.new In("") {}.e.toString();
        }
    }
}
