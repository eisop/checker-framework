package org.checkerframework.common.util.count;

import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Tests for {@link AnnotationStatistics} and {@link JavaCodeStatistics}.
 *
 * <p>Regression test for issue #2089: {@code AnnotationStatistics} crashed with a {@link
 * NullPointerException} in {@code SourceChecker.setRoot} because {@code treePathCacher} was null,
 * and {@code JavaCodeStatistics} failed to instantiate because {@code TreeUtils.getMethod} was
 * called at field initialization time before {@code processingEnv} was initialized.
 */
public class StatisticsTest {

    /**
     * Returns an in-memory Java source file.
     *
     * @param className the qualified or simple name of the class, used to form the file URI
     * @param code the source text of the file
     * @return a {@link JavaFileObject} that yields {@code code} as its content
     */
    private static JavaFileObject source(String className, String code) {
        return new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + ".java"),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }

    /**
     * Tests running {@link AnnotationStatistics} on unannotated code.
     *
     * <p>Regression test for issue #2089: verifies that running {@code AnnotationStatistics} on
     * classes without annotations succeeds without throwing a {@link NullPointerException}.
     */
    @Test
    public void testAnnotationStatisticsOnUnannotatedCode() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        AnnotationStatistics processor = new AnnotationStatistics();
        CompilationTask task =
                compiler.getTask(
                        new StringWriter(),
                        null,
                        diagnostics,
                        Arrays.asList("-proc:only", "-Aannotations"),
                        null,
                        Collections.singletonList(
                                source(
                                        "pkg.InPkg",
                                        "package pkg;\npublic class InPkg { Object f; }\n")));
        task.setProcessors(Collections.singletonList(processor));
        boolean success = task.call();
        Assert.assertTrue("Compilation should succeed", success);
        Assert.assertTrue(diagnostics.getDiagnostics().isEmpty());
        Assert.assertTrue(processor.annotationCount.isEmpty());
    }

    /**
     * Tests running {@link AnnotationStatistics} on annotated code.
     *
     * <p>Verifies that {@link AnnotationStatistics} correctly counts occurrences of annotations on
     * classes and fields.
     */
    @Test
    public void testAnnotationStatisticsCountsAnnotations() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        AnnotationStatistics processor = new AnnotationStatistics();
        CompilationTask task =
                compiler.getTask(
                        new StringWriter(),
                        null,
                        diagnostics,
                        Arrays.asList("-proc:only", "-Aannotations"),
                        null,
                        Collections.singletonList(
                                source(
                                        "pkg.AnnotatedClass",
                                        "package pkg;\n"
                                                + "@Deprecated\n"
                                                + "public class AnnotatedClass {\n"
                                                + "    @Deprecated Object f1;\n"
                                                + "    @SuppressWarnings(\"unchecked\") Object f2;\n"
                                                + "}\n")));
        task.setProcessors(Collections.singletonList(processor));
        boolean success = task.call();
        Assert.assertTrue("Compilation should succeed", success);
        Assert.assertEquals(2, (int) processor.annotationCount.get("java.lang.Deprecated"));
        Assert.assertEquals(1, (int) processor.annotationCount.get("java.lang.SuppressWarnings"));
    }

    /**
     * Tests running {@link JavaCodeStatistics} on Java code.
     *
     * <p>Regression test for issue #2089: verifies that {@code JavaCodeStatistics} instantiates and
     * counts code constructs without failing during initialization.
     */
    @Test
    public void testJavaCodeStatistics() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCodeStatistics processor = new JavaCodeStatistics();
        CompilationTask task =
                compiler.getTask(
                        new StringWriter(),
                        null,
                        diagnostics,
                        Collections.singletonList("-proc:only"),
                        null,
                        Collections.singletonList(
                                source(
                                        "pkg.StatsTest",
                                        "package pkg;\n"
                                                + "import java.util.List;\n"
                                                + "public class StatsTest<T> {\n"
                                                + "    @SuppressWarnings(\"index\")\n"
                                                + "    void m(List<String> list) {\n"
                                                + "        String s = (String) list.get(0);\n"
                                                + "        int[] arr = new int[5];\n"
                                                + "        int x = arr[0];\n"
                                                + "    }\n"
                                                + "}\n")));
        task.setProcessors(Collections.singletonList(processor));
        boolean success = task.call();
        Assert.assertTrue("Compilation should succeed", success);
        Assert.assertEquals(1, processor.typecasts);
        Assert.assertTrue(processor.generics > 0);
        Assert.assertTrue(processor.arrayAccesses > 0);
        Assert.assertEquals(1, processor.numberOfIndexWarningSuppressions);
    }
}
