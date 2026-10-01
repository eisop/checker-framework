import org.checkerframework.framework.testchecker.util.Odd;

/**
 * Regression test for the default behavior of {@link
 * org.checkerframework.common.basetype.BaseTypeVisitor#reportTypeArgumentInferenceFailure}: {@link
 * Ordering2#sort} cannot be called on {@code e} without an explicit type argument because the
 * Checker Framework's annotation-level type argument inference finds no satisfying instantiation,
 * even though javac's own (unannotated) type argument inference succeeds for the same invocation.
 * Adapted from the {@code Ordering2} case in {@code GenericTest9.java}. By default this remains a
 * hard error.
 */
public class TypeArgumentInferenceFailure {

    /** A generic entry type used only to build the failing-inference example below. */
    interface MyEntry<S> {}

    /**
     * A generic type whose type parameter has an annotated upper bound. Instantiating {@code sort}
     * against {@code MyEntry<V>} is what makes annotation-level inference fail.
     *
     * @param <T> the upper bound for the argument to {@link #sort}
     */
    interface Ordering2<T extends @Odd Object> {
        /**
         * Returns its argument unchanged.
         *
         * @param <U> the type of {@code iterable}, a subtype of {@code T}
         * @param iterable the value to return
         * @return {@code iterable}
         */
        <U extends T> U sort(U iterable);
    }

    /**
     * Triggers the type-argument-inference failure described in the class Javadoc.
     *
     * @param <V> an unbounded type variable
     * @param ord an {@code Ordering2} instantiated with a {@code MyEntry<?>} bound
     * @param e the value passed to {@link Ordering2#sort}
     */
    <V> void test(Ordering2<@Odd MyEntry<?>> ord, MyEntry<V> e) {
        // :: error: (type.arguments.not.inferred)
        MyEntry<V> e1 = ord.sort(e);
    }
}
