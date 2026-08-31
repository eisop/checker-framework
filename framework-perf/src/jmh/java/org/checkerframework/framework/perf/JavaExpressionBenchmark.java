package org.checkerframework.framework.perf;

import org.checkerframework.dataflow.expression.LocalVariable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Benchmarks for {@link LocalVariable} and other {@code JavaExpression} subclasses whose {@code
 * hashCode} caching was added in PR #1643. The cache pays back on dataflow store comparisons where
 * the same expression is hashed repeatedly across propagation steps.
 *
 * <p>This benchmark intentionally uses {@link LocalVariable} only; the cache pattern is symmetric
 * across {@code ArrayAccess}, {@code ArrayCreation}, {@code BinaryOperation}, {@code FieldAccess},
 * {@code FormalParameter}, {@code MethodCall}, {@code UnaryOperation}, and {@code ValueLiteral}.
 * Add additional benchmarks here if a specific subclass shows up in a JFR trace; the bare {@code
 * LocalVariable} numbers are the cleanest reference point.
 */
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class JavaExpressionBenchmark {

    /** Thread-scoped state holding a real javac {@link VariableElement} and a built expression. */
    @State(Scope.Thread)
    public static class LocalVarState {
        public VariableElement element;
        public LocalVariable lv;

        @Setup(Level.Trial)
        public void setUp(BaseJavacState base) {
            // String has several public static final fields (CASE_INSENSITIVE_ORDER); pick any
            // field for a realistic VariableElement.
            TypeElement string = base.env.getElementUtils().getTypeElement("java.lang.String");
            VariableElement chosen = null;
            for (Element e : string.getEnclosedElements()) {
                if (e.getKind() == ElementKind.FIELD) {
                    chosen = (VariableElement) e;
                    break;
                }
            }
            if (chosen == null) {
                throw new IllegalStateException("java.lang.String has no fields?");
            }
            element = chosen;
            lv = new LocalVariable(element);
            // Warm the hashCode cache so the cached-path benchmark doesn't measure first-call cost.
            lv.hashCode();
        }
    }

    /** Cached path: the second and subsequent calls should return the cached value. */
    @Benchmark
    public int hashCode_cached(LocalVarState s) {
        return s.lv.hashCode();
    }

    /** Cold path: construct a fresh {@link LocalVariable} and compute its hash once. */
    @Benchmark
    public int hashCode_cold(LocalVarState s) {
        return new LocalVariable(s.element).hashCode();
    }

    /** Equality: cheap reference-equality short-circuit added in PR #1644. */
    @Benchmark
    public boolean equals_self(LocalVarState s) {
        return s.lv.equals(s.lv);
    }
}
