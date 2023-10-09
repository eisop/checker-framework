// 6/1/2023: We changed the defaulting rules despite the issues described below, because we
// discovered a soundness bug that demonstrated that the only reason the other defaulting scheme
// didn't produce a lot of false positive errors was a mistake in our implementation:
// both defaulting schemes produce (different) false positives, but defaulting to @MustCall({}) on
// the upper bound of type variables limits the extra warnings to code that actually uses resources,
// which is desirable. As part of this change, the expected warnings in this test case were removed,
// but I have preserved their locations with "a type.argument error used to be issued here".
// The following comment is the original note attached to this test case:

// This is a test case that shows off some places in plume-util where
// annotations were required, even though we'd have preferred the defaulting
// rules to result in those annotations being the defaults.
// See the discussion on https://github.com/kelloggm/object-construction-checker/pull/363
// and https://github.com/plume-lib/plume-util/pull/126 for more details, especially
// on why changing the default isn't feasible.

import java.util.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.Nullable;

class PlumeUtilRequiredAnnotations {
  // In the real version of this code, there is only one type parameter.
  // T is the unannotated version of the parameter - i.e., what it was before
  // we first ran the Must Call Checker. S is the annotated version. Adding the
  // annotation itself is immaterial - what's important is that the bound
  // must be explicit rather than implicit (see that the eqR field never issue errors,
  // just like the eqS fields).
  class MultiRandSelector<T, S extends @Nullable @MustCall Object, R extends Object> {
    // a type.argument error used to be issued here.
    private Partitioner<T, T> eqT;
    private Partitioner<S, S> eqS;
    private Partitioner<R, R> eqR;

    // Adding annotations to the definition of Partitioner doesn't fix this problem:
    // a type.argument error used to be issued here.
    private Partitioner2<T, T> eqT2;
    private Partitioner2<S, S> eqS2;
    private Partitioner2<R, R> eqR2;

    // But removing the explicit bounds on Partitioner does (not feasible in this case, though,
    // because of the @Nullable annotations):
    private Partitioner3<T, T> eqT3;
    private Partitioner3<S, S> eqS3;
    private Partitioner3<R, R> eqR3;
  }

  interface Partitioner<ELEMENT extends @Nullable Object, CLASS extends @Nullable Object> {
    CLASS assignToBucket(ELEMENT obj);
  }

  interface Partitioner2<
      ELEMENT extends @Nullable @MustCall Object, CLASS extends @Nullable @MustCall Object> {
    CLASS assignToBucket(ELEMENT obj);
  }

  interface Partitioner3<ELEMENT, CLASS> {
    CLASS assignToBucket(ELEMENT obj);
  }
}
