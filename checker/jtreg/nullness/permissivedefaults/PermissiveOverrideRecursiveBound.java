/*
 * @test
 *
 * @summary Test that an override check under permissive defaults terminates when a type variable in the signature of the unchecked overridden method has a recursive bound.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source PermissiveOverrideRecursiveBound.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify PermissiveOverrideRecursiveBound.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveOverrideRecursiveBound.java
 */

import org.checkerframework.framework.qual.AnnotatedFor;

/** Unchecked code: not {@code @AnnotatedFor("nullness")}. */
class RecursiveBoundLib {
    /** An interface whose type variable is used in its own bound. */
    interface Entry<E extends Entry<E>> {
        E getNext();

        void setNext(E next);
    }

    /** An interface whose type variables are used in each other's bounds. */
    interface Pair<A extends Pair<A, B>, B extends Pair<B, A>> {
        A first();

        B second();
    }

    /** A class with a method whose type variable is used in its own bound. */
    static class Ordering {
        <T extends Comparable<T>> T max(T a, T b) {
            return a;
        }
    }
}

@AnnotatedFor("nullness")
public class PermissiveOverrideRecursiveBound {
    static class Strong implements RecursiveBoundLib.Entry<Strong> {
        @Override
        public Strong getNext() {
            return this;
        }

        @Override
        public void setNext(Strong next) {}
    }

    // A covariant return type.
    static class Linked extends Strong {
        @Override
        public Linked getNext() {
            return this;
        }
    }

    // An override of a method of a raw supertype, with a covariant return type.
    @SuppressWarnings("rawtypes")
    static class RawEntry implements RecursiveBoundLib.Entry {
        @Override
        public Strong getNext() {
            return new Strong();
        }

        @Override
        public void setNext(RecursiveBoundLib.Entry next) {}
    }

    static class First implements RecursiveBoundLib.Pair<First, Second> {
        @Override
        public First first() {
            return this;
        }

        @Override
        public Second second() {
            return new Second();
        }
    }

    static class Second implements RecursiveBoundLib.Pair<Second, First> {
        @Override
        public Second first() {
            return this;
        }

        @Override
        public First second() {
            return new First();
        }
    }

    // An override with the same generic signature.
    static class GenericOrdering extends RecursiveBoundLib.Ordering {
        @Override
        <T extends Comparable<T>> T max(T a, T b) {
            return a;
        }
    }

    // An override by the erasure of the method.
    @SuppressWarnings("rawtypes")
    static class RawOrdering extends RecursiveBoundLib.Ordering {
        @Override
        Comparable max(Comparable a, Comparable b) {
            return a;
        }
    }
}
