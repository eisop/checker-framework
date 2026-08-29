// Regression test: a (since-reverted) elementTypeCache "boundary flip" returned the shared
// frozen cache master from getAnnotatedType(Element) instead of a deep copy. A consumer then
// embedded a *sub-component* of that shared value -- here an unbounded wildcard's implicit
// java.lang.Object upper bound, derived from a JDK generic type's cached type-parameter bound --
// into a fresh, non-frozen result type. Because the root was not frozen, the copy-on-frozen
// guards (which check only the root) did not copy it, and addComputedTypeAnnotations later
// mutated the frozen child, crashing with "Attempted to mutate a frozen AnnotatedTypeMirror with
// underlying type java.lang.Object".
//
// It needs a JDK parameterized type (whose annotated type is cached) used with an unbounded
// wildcard, plus a raw-generic-bounded type variable, in a generic method that is invoked so the
// dataflow transfer re-derives the local's type. Minimized from Guava's
// com.google.common.collect.SortedLists.binarySearch, which alltests did not cover. This test
// should type-check with no errors.

import java.util.function.Function;

public class ElementTypeCacheWildcardBound {
    @SuppressWarnings("rawtypes")
    static <K extends Comparable> int f(Function<?, K> fn) {
        return g(fn);
    }

    @SuppressWarnings("rawtypes")
    static <K extends Comparable> int g(Function<?, K> fn) {
        return 0;
    }
}
