package org.checkerframework.framework.perf;

import org.checkerframework.javacutil.AnnotationUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

import javax.lang.model.element.Name;

/**
 * Benchmarks for {@link AnnotationUtils} hot methods.
 *
 * <p>{@code annotationName} is called transitively from {@code findAnnotationInSameHierarchy},
 * {@code isSupportedQualifier}, and most LUB/GLB call sites. PR #1673 made it return an interned
 * String backed by an identity-keyed cache; PR #1669 added {@code annotationNameAsName} for hot
 * paths that need only the underlying {@code Name}.
 */
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AnnotationUtilsBenchmark {

    /** Hot path: identity comparison of two name strings from the cache. */
    @Benchmark
    public boolean annotationName_areSameByName(BaseJavacState base) {
        // Both calls hit the per-factory cache after the first warmup iteration; comparison
        // should reduce to ==.
        return AnnotationUtils.annotationName(base.anno1)
                == AnnotationUtils.annotationName(base.anno1);
    }

    /** Cost of {@code annotationName} on a cold pair (different annotations). */
    @Benchmark
    public boolean annotationName_differentAnnotations(BaseJavacState base) {
        return AnnotationUtils.annotationName(base.anno1)
                .equals(AnnotationUtils.annotationName(base.anno2));
    }

    /** {@code annotationNameAsName} skips the {@code Name.toString} decode entirely. */
    @Benchmark
    public Name annotationNameAsName(BaseJavacState base) {
        return AnnotationUtils.annotationNameAsName(base.anno1);
    }

    /** {@code areSame} on identical marker annotations: the empty-elements short-circuit. */
    @Benchmark
    public boolean areSame_sameMarker(BaseJavacState base) {
        return AnnotationUtils.areSame(base.anno1, base.anno1);
    }

    /** {@code areSame} on distinct marker annotations: hits the name comparison path. */
    @Benchmark
    public boolean areSame_differentMarkers(BaseJavacState base) {
        return AnnotationUtils.areSame(base.anno1, base.anno2);
    }
}
