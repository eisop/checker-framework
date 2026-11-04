/*
 * @test
 * @summary Test that a checker-qual of another Checker Framework earlier on the annotation
 *     processor path is reported once, with the location it was loaded from, instead of every
 *     compilation unit failing with a NoSuchFieldError.
 *     https://github.com/eisop/checker-framework/issues/2372
 *
 * @compile ShadowedCheckerQual.java
 * @run main ShadowedCheckerQual
 */

import org.checkerframework.framework.source.SourceChecker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Runs javac in a separate process: an in-process javac finds the Checker Framework's classes on
 * this test's class path before the processor path, so only a separate process shows the effect of
 * the order of the processor path.
 */
public class ShadowedCheckerQual {

    /** The distinctive part of the expected error message. */
    private static final String MESSAGE = "earlier on the annotation processor path";

    public static void main(String[] args) throws Exception {
        Path shadow = Paths.get(System.getProperty("test.classes", "."), "shadowing-qual");
        compileShadowingTypeUseLocation(shadow);
        Path checkerJar = jarOf(SourceChecker.class);
        File input = new File(System.getProperty("test.src", "."), "Input.java");

        // The other checker-qual first: one error naming where the qualifiers came from.
        List<String> shadowed = run(shadow + File.pathSeparator + checkerJar, checkerJar, input);
        List<String> errors = errorLines(shadowed);
        if (errors.size() != 1
                || !errors.get(0).contains(MESSAGE)
                || !errors.get(0).contains("shadowing-qual")) {
            throw new AssertionError("expected one error about the processor path: " + shadowed);
        }

        // checker.jar first: the checker runs and reports the one nullness error of Input.java.
        List<String> control = run(checkerJar + File.pathSeparator + shadow, checkerJar, input);
        errors = errorLines(control);
        if (errors.size() != 1 || !errors.get(0).contains("dereference.of.nullable")) {
            throw new AssertionError("expected one dereference.of.nullable error: " + control);
        }
    }

    /**
     * Returns the lines of javac's output that report an error.
     *
     * @param output javac's output
     * @return the error lines
     */
    private static List<String> errorLines(List<String> output) {
        List<String> errors = new ArrayList<>();
        for (String line : output) {
            if (line.contains("error: ")) {
                errors.add(line);
            }
        }
        return errors;
    }

    /**
     * Runs the Nullness Checker on {@code input} in a separate javac process with the given
     * annotation processor path, and returns javac's output.
     *
     * @param processorPath the annotation processor path
     * @param classPath the compilation class path
     * @param input the file to check
     * @return the lines javac printed
     * @throws IOException if javac cannot be started
     * @throws InterruptedException if waiting for javac is interrupted
     */
    private static List<String> run(String processorPath, Path classPath, File input)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "javac").toString());
        // The Checker Framework needs access to javac's internals.  (jtreg runs on JDK 11 and
        // later.)
        for (String pkg :
                Arrays.asList(
                        "api",
                        "code",
                        "comp",
                        "file",
                        "main",
                        "parser",
                        "processing",
                        "tree",
                        "util")) {
            command.add("-J--add-opens=jdk.compiler/com.sun.tools.javac." + pkg + "=ALL-UNNAMED");
        }
        command.addAll(
                Arrays.asList(
                        "-processorpath",
                        processorPath,
                        "-processor",
                        "org.checkerframework.checker.nullness.NullnessChecker",
                        "-classpath",
                        classPath.toString(),
                        "-proc:only",
                        input.getPath()));
        Process javac = new ProcessBuilder(command).redirectErrorStream(true).start();
        List<String> output = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(javac.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        }
        javac.waitFor();
        return output;
    }

    /**
     * Compiles a {@code TypeUseLocation} that lacks the constants of this release into {@code
     * dest}, as the checker-qual of another Checker Framework has.
     *
     * @param dest the directory for the class file
     * @throws IOException if writing the source fails
     */
    private static void compileShadowingTypeUseLocation(Path dest) throws IOException {
        Path src = dest.resolve("src/org/checkerframework/framework/qual/TypeUseLocation.java");
        Files.createDirectories(src.getParent());
        Files.write(
                src,
                Arrays.asList(
                        "package org.checkerframework.framework.qual;",
                        "public enum TypeUseLocation { FIELD, LOCAL_VARIABLE, PARAMETER, RETURN,"
                                + " LOWER_BOUND, UPPER_BOUND, OTHERWISE, ALL }"),
                StandardCharsets.UTF_8);
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        int result =
                javac.run(null, null, null, "-d", dest.toString(), "-proc:none", src.toString());
        if (result != 0) {
            throw new AssertionError("compiling the shadowing TypeUseLocation failed");
        }
    }

    /**
     * Returns the jar that a class was loaded from.
     *
     * @param c a class
     * @return its jar
     * @throws URISyntaxException if the location is not a URI
     */
    private static Path jarOf(Class<?> c) throws URISyntaxException {
        Path jar = Paths.get(c.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!jar.toString().endsWith(".jar")) {
            throw new AssertionError("expected " + c + " in a jar, found it in " + jar);
        }
        return jar;
    }
}
