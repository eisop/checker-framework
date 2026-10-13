/*
 * @test
 * @summary Test that the Nullness Checker runs when the compilation classpath has a checker-qual
 *   that lacks PolyInitialized, as other checker-qual artifacts do.
 *   https://github.com/eisop/checker-framework/issues/2146
 *
 * @compile MissingPolyInitialized.java
 * @run main MissingPolyInitialized
 */

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.NullnessChecker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class MissingPolyInitialized {

    /** The class file that the compilation classpath lacks. */
    private static final String POLY_INITIALIZED =
            "org/checkerframework/checker/initialization/qual/PolyInitialized.class";

    public static void main(String[] args) throws Exception {
        Path qual = Paths.get(System.getProperty("test.classes", "."), "qual-without-poly");
        copyQualifiersExceptPolyInitialized(qual);

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);
        File input = new File(System.getProperty("test.src", "."), "Input.java");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task =
                javac.getTask(
                        null,
                        fileManager,
                        diagnostics,
                        Arrays.asList("-classpath", qual.toString(), "-proc:only"),
                        null,
                        fileManager.getJavaFileObjects(input));
        task.setProcessors(Arrays.asList(new NullnessChecker()));
        task.call();

        int polyNotes = 0;
        List<String> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            String message = d.getMessage(null);
            if (d.getKind() == Diagnostic.Kind.NOTE
                    && message.contains("@PolyInitialized is not on the classpath")) {
                polyNotes++;
            } else if (d.getKind() == Diagnostic.Kind.ERROR) {
                errors.add(message);
            }
        }
        if (polyNotes != 1) {
            throw new AssertionError(
                    "expected one note about PolyInitialized, found "
                            + polyNotes
                            + ": "
                            + diagnostics.getDiagnostics());
        }
        // The checker still type-checks: Input.java has exactly one nullness error.
        if (errors.size() != 1 || !errors.get(0).contains("return.type.incompatible")) {
            throw new AssertionError("expected one return.type.incompatible error: " + errors);
        }
    }

    /**
     * Copies the qualifier classes from the jar that contains the Checker Framework's qualifiers
     * into {@code dest}, except for {@code PolyInitialized}.
     *
     * @param dest the directory to copy to
     * @throws IOException if reading the jar or writing a class file fails
     */
    private static void copyQualifiersExceptPolyInitialized(Path dest) throws IOException {
        Path jar;
        try {
            jar =
                    Paths.get(
                            Initialized.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI());
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
        if (!jar.toString().endsWith(".jar")) {
            throw new AssertionError("expected the qualifiers in a jar, found them in " + jar);
        }
        boolean sawPolyInitialized = false;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()
                        || !name.startsWith("org/checkerframework/")
                        || !name.contains("/qual/")) {
                    continue;
                }
                if (name.equals(POLY_INITIALIZED)) {
                    sawPolyInitialized = true;
                    continue;
                }
                Path target = dest.resolve(name);
                Files.createDirectories(target.getParent());
                try (InputStream in = zip.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        if (!sawPolyInitialized) {
            throw new AssertionError(POLY_INITIALIZED + " is not in " + jar);
        }
    }
}
