package org.checkerframework.framework.perf;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;

import org.checkerframework.javacutil.AnnotationBuilder;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

/**
 * Trial-scoped JMH state that boots a minimal javac {@link ProcessingEnvironment} so benchmarks can
 * construct real {@link AnnotationMirror} instances.
 *
 * <p>Pattern borrowed from {@code framework/src/test/java/.../AnnotationBuilderTest.java}. The
 * processing environment is created once per JMH fork and reused across all iterations; setup cost
 * is amortized away from the measured region.
 */
@State(Scope.Benchmark)
public class BaseJavacState {

    /** The javac processing environment used to construct {@link AnnotationMirror} instances. */
    public ProcessingEnvironment env;

    /** Three distinct annotations, useful as test data for set/map operations. */
    public AnnotationMirror anno1;

    /** Two distinct annotations, useful as test data for set/map operations. */
    public AnnotationMirror anno2;

    /** Three distinct annotations, useful as test data for set/map operations. */
    public AnnotationMirror anno3;

    @Setup(Level.Trial)
    public void setUp() {
        Context context = new Context();
        Options options = Options.instance(context);
        options.put(Option.SOURCE, "8");
        options.put(Option.TARGET, "8");
        env = JavacProcessingEnvironment.instance(context);
        JavaCompiler javac = JavaCompiler.instance(context);
        javac.initModules(List.nil());
        javac.enterDone();

        // Three platform annotations available without any CF test fixture. Their identity is
        // what matters for AnnotationMirrorSet operations, not their semantics.
        anno1 = new AnnotationBuilder(env, Deprecated.class).build();
        anno2 = new AnnotationBuilder(env, Override.class).build();
        anno3 = new AnnotationBuilder(env, SuppressWarnings.class).build();
    }
}
