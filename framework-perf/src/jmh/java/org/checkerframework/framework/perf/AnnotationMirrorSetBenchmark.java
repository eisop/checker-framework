package org.checkerframework.framework.perf;

import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import javax.lang.model.element.AnnotationMirror;

/**
 * Benchmarks for {@link AnnotationMirrorSet}. This was the highest-yield target of the April–May
 * 2026 optimization campaign (PR #1638, #1641, #1649, #1669) and is on every dataflow hot path.
 *
 * <p>Benchmarks here exercise the typical small-set sizes observed in profiling (1–4 elements). If
 * you add a benchmark with a larger size, document why — large sets are unusual in practice and
 * dominating perf decisions on them risks regressing the common case.
 */
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AnnotationMirrorSetBenchmark {

    /** Pre-populated source sets reused across iterations of the {@code add*} benchmarks. */
    @State(Scope.Thread)
    public static class SourceSets {
        public AnnotationMirrorSet empty;
        public AnnotationMirrorSet onePresent;
        public AnnotationMirrorSet twoElements;

        @Setup
        public void setUp(BaseJavacState base) {
            empty = new AnnotationMirrorSet();

            onePresent = new AnnotationMirrorSet();
            onePresent.add(base.anno1);

            twoElements = new AnnotationMirrorSet();
            twoElements.add(base.anno1);
            twoElements.add(base.anno2);
        }
    }

    /** Cost of adding a single new element to an empty set. */
    @Benchmark
    public AnnotationMirrorSet add_emptyToOne(BaseJavacState base) {
        AnnotationMirrorSet s = new AnnotationMirrorSet();
        s.add(base.anno1);
        return s;
    }

    /** Cost of adding to a set that already contains the element (returns {@code false}). */
    @Benchmark
    public boolean add_alreadyPresent(BaseJavacState base, SourceSets src) {
        return src.onePresent.add(base.anno1);
    }

    /** Cost of building a typical 3-element set from scratch. */
    @Benchmark
    public AnnotationMirrorSet add_buildThree(BaseJavacState base) {
        AnnotationMirrorSet s = new AnnotationMirrorSet();
        s.add(base.anno1);
        s.add(base.anno2);
        s.add(base.anno3);
        return s;
    }

    /** {@code addAll} fast-path: argument is itself an AnnotationMirrorSet (the common case). */
    @Benchmark
    public boolean addAll_amSet(BaseJavacState base, SourceSets src) {
        AnnotationMirrorSet target = new AnnotationMirrorSet();
        target.add(base.anno1);
        return target.addAll(src.twoElements);
    }

    /** Iteration cost across the typical small size. */
    @Benchmark
    public void iterate_three(BaseJavacState base, SourceSets src, Blackhole bh) {
        AnnotationMirrorSet s = new AnnotationMirrorSet();
        s.add(base.anno1);
        s.add(base.anno2);
        s.add(base.anno3);
        for (AnnotationMirror m : s) {
            bh.consume(m);
        }
    }

    /** Set hashCode: this is read on every dataflow map operation. */
    @Benchmark
    public int hashCode_three(BaseJavacState base) {
        AnnotationMirrorSet s = new AnnotationMirrorSet();
        s.add(base.anno1);
        s.add(base.anno2);
        s.add(base.anno3);
        return s.hashCode();
    }
}
