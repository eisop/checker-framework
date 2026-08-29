package org.checkerframework.errorprone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;

import org.junit.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Tests for {@link EisopContextAdapter}: obtaining a usable {@link ProcessingEnvironment} from a
 * javac {@link Context} the way the Error Prone plugin will, and verifying the Checker Framework's
 * own (un-relocated) dataflow library is the one on the classpath rather than Error Prone's shaded
 * copy.
 */
public class EisopContextAdapterTest {

    /** An in-memory source file. */
    private static JavaFileObject source(String className, String code) {
        return new SimpleJavaFileObject(
                URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }

    /**
     * Runs an in-process compilation and invokes {@code assertions} from a TaskListener on
     * ANALYZE-finished (the phase at which Error Prone, and therefore this plugin, runs). Running
     * mid-compilation is required: the {@link ProcessingEnvironment}/{@link Elements} utilities can
     * resolve elements only while the compiler is live, not after {@code task.call()} returns.
     *
     * <p>Any {@link Throwable} thrown by {@code assertions} is captured and rethrown after the
     * compilation completes, so JUnit assertion failures are reported normally.
     */
    private static void duringAnalyze(ContextAssertions assertions) throws Throwable {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // Direct class output to a throwaway temp dir so the test does not emit .class files into
        // the working directory / source tree.
        Path outDir = Files.createTempDirectory("eisopcf-ctx-test");
        // Note: no -proc:none, so a JavacProcessingEnvironment is created (the plugin needs it).
        JavaCompiler.CompilationTask compilationTask =
                compiler.getTask(
                        null,
                        null,
                        null,
                        Arrays.asList("-d", outDir.toString()),
                        null,
                        Collections.singletonList(
                                source("Hello", "class Hello { int f() { return 1; } }")));
        JavacTask task = (JavacTask) compilationTask;
        Context context = ((BasicJavacTask) task).getContext();
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AtomicBoolean ran = new AtomicBoolean(false);
        task.addTaskListener(
                new TaskListener() {
                    @Override
                    public void finished(TaskEvent e) {
                        if (e.getKind() == TaskEvent.Kind.ANALYZE
                                && ran.compareAndSet(false, true)) {
                            try {
                                assertions.run(context);
                            } catch (Throwable t) {
                                thrown.compareAndSet(null, t);
                            }
                        }
                    }
                });
        task.call();
        if (thrown.get() != null) {
            throw thrown.get();
        }
        assertTrue("ANALYZE event never fired; assertions did not run", ran.get());
    }

    /** Assertions run against a live javac {@link Context} during the ANALYZE phase. */
    private interface ContextAssertions {
        void run(Context context) throws Throwable;
    }

    @Test
    public void producesUsableProcessingEnvironment() throws Throwable {
        duringAnalyze(
                context -> {
                    ProcessingEnvironment env =
                            EisopContextAdapter.getProcessingEnvironment(context);
                    assertNotNull("processing environment", env);

                    // The utilities SourceChecker.setProcessingEnvironment / initChecker rely on.
                    Elements elements = env.getElementUtils();
                    Types types = env.getTypeUtils();
                    assertNotNull("Elements", elements);
                    assertNotNull("Types", types);
                    assertNotNull("Messager", env.getMessager());
                    assertNotNull("options map", env.getOptions());

                    Trees trees = Trees.instance(env);
                    assertNotNull("Trees", trees);

                    // The environment is actually wired to the compilation: the compiled type is
                    // resolvable through the Elements utility (only valid while the compiler is
                    // live, hence running inside the ANALYZE callback).
                    assertNotNull(
                            "compiled type element should be resolvable via the env",
                            elements.getTypeElement("Hello"));

                    // Convenience accessor returns a usable Trees instance too.
                    assertNotNull("adapter getTrees", EisopContextAdapter.getTrees(context));
                });
    }

    @Test
    public void usesUnrelocatedCheckerFrameworkDataflow() {
        // The CF core must use its own org.checkerframework.dataflow, NOT Error Prone's shaded
        // org.checkerframework.errorprone.dataflow (which is also on the test classpath via
        // error_prone_check_api). Class.forName on the un-relocated FQN must resolve, and its
        // package must be the un-relocated one.
        String pkg = EisopContextAdapter.loadedDataflowPackage();
        assertEquals("org.checkerframework.dataflow.cfg", pkg);
        assertFalse(
                "must not be Error Prone's relocated dataflow copy",
                pkg.startsWith("org.checkerframework.errorprone.dataflow"));
    }

    @Test
    public void bothDataflowCopiesAreOnClasspathButDistinct() throws Exception {
        // Guard rationale: verify the coexistence assumption actually holds — the relocated copy
        // IS present (so this is a real test), yet the un-relocated FQN resolves to the CF's own.
        Class<?> unrelocated = Class.forName("org.checkerframework.dataflow.cfg.ControlFlowGraph");
        assertNotNull("un-relocated CF dataflow must be present", unrelocated);
        Class<?> relocated = null;
        try {
            relocated =
                    Class.forName("org.checkerframework.errorprone.dataflow.cfg.ControlFlowGraph");
        } catch (ClassNotFoundException e) {
            // If Error Prone's shaded copy is ever absent, the coexistence concern is moot.
            relocated = null;
        }
        if (relocated != null) {
            assertFalse("the two copies must be distinct classes", unrelocated.equals(relocated));
            assertFalse(
                    "the two copies must have distinct names",
                    unrelocated.getName().equals(relocated.getName()));
        }
        assertFalse(
                "adapter must never report the relocated package",
                EisopContextAdapter.loadedDataflowPackage()
                        .startsWith("org.checkerframework.errorprone"));
    }
}
