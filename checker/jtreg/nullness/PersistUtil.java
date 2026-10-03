// Note that "-processor org/checkerframework/checker.nullness.NullnessChecker"
// is added to the invocation of the compiler!
// TODO: add a @Processor method-annotation to parameterize

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.StringJoiner;

/**
 * Auxiliary methods to compile a Java snippet with the Nullness Checker and return the resulting
 * class file. Used by both the {@code com.sun.tools.classfile} (JDK &lt;= 24) and {@code
 * java.lang.classfile} (JDK &gt;= 25) bytecode test harnesses.
 *
 * <p>Used by the test drivers {@code checker/jtreg/nullness/defaultsPersist/Driver.java}, {@code
 * checker/jtreg/nullness/defaultsPersist25/Driver.java}, {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist/Driver.java}, and {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist25/Driver.java}.
 */
public class PersistUtil {

    /** Private constructor to prevent instantiation of utility class. */
    private PersistUtil() {}

    /**
     * Returns the name of the test class to inspect for a given test method. If the method is
     * annotated with {@link TestClass}, returns its value; otherwise defaults to {@code "Test"}.
     *
     * @param m the test method
     * @return the name of the class to inspect
     */
    public static String testClassOf(Method m) {
        TestClass tc = m.getAnnotation(TestClass.class);
        if (tc != null) {
            return tc.value();
        } else {
            return "Test";
        }
    }

    /**
     * Compiles a full Java source file with {@code NullnessChecker} enabled and returns the
     * resulting {@code .class} file.
     *
     * @param fullFile the full Java source code to compile
     * @param testClass the name of the class whose {@code .class} file should be returned
     * @return the compiled {@code .class} file
     * @throws IOException if writing the source file fails
     */
    public static File compile(String fullFile, String testClass) throws IOException {
        File source = writeTestFile(fullFile);
        return compileTestFile(source, testClass);
    }

    /**
     * Writes the given source string to a file named {@code Test.java} in the current directory.
     *
     * @param fullFile the Java source code to write
     * @return the written file
     * @throws IOException if writing the file fails
     */
    public static File writeTestFile(String fullFile) throws IOException {
        File f = new File("Test.java");
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
            out.println(fullFile);
        }
        return f;
    }

    /**
     * Compiles the source file using {@code com.sun.tools.javac.Main} with the {@code
     * NullnessChecker} annotation processor enabled.
     *
     * @param f the source file to compile
     * @param testClass the name of the class whose {@code .class} file should be returned
     * @return the compiled {@code .class} file
     */
    public static File compileTestFile(File f, String testClass) {
        int rc =
                com.sun.tools.javac.Main.compile(
                        new String[] {
                            "-AnoJreVersionCheck",
                            "-g",
                            "-processor",
                            "org.checkerframework.checker.nullness.NullnessChecker",
                            f.getPath()
                        });
        if (rc != 0) {
            throw new Error("compilation failed. rc=" + rc);
        }

        File result = new File(f.getParent(), testClass + ".class");

        // Uncomment for debugging:
        // copyFilesForDebugging(f, result);

        return result;
    }

    /**
     * Preserves copies of the source and class files in the system temporary directory for
     * debugging purposes and prints their paths to standard output.
     *
     * @param source the source file
     * @param classFile the compiled class file
     */
    @SuppressWarnings("unused")
    private static void copyFilesForDebugging(File source, File classFile) {
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File sourceCopy = File.createTempFile("FCopy", ".java", tempDir);
            File classCopy = File.createTempFile("FCopy", ".class", tempDir);
            // REPLACE_EXISTING is essential in the `Files.copy()` calls because createTempFile
            // actually creates a file in addition to returning its name.
            Files.copy(source.toPath(), sourceCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(classFile.toPath(), classCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("compileTestFile: copied to %s %s%n", sourceCopy, classCopy);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Wraps a compact test snippet with standard imports and, if the snippet is not already a
     * class, interface, or enum declaration, encloses it in {@code class Test { ... }}.
     *
     * @param compact the snippet or class declaration
     * @return the full Java source string
     */
    public static String wrap(String compact) {
        StringJoiner sj = new StringJoiner(System.lineSeparator());

        // Automatically import java.util
        sj.add("");
        sj.add("import java.util.*;");
        sj.add("import java.lang.annotation.*;");

        // And the Nullness qualifiers
        sj.add("import org.checkerframework.framework.qual.DefaultQualifier;");
        sj.add("import org.checkerframework.checker.nullness.qual.*;");
        sj.add("import org.checkerframework.dataflow.qual.*;");

        sj.add("");
        boolean isSnippet =
                !(compact.startsWith("class") || compact.contains(" class"))
                        && !compact.contains("interface")
                        && !compact.contains("enum");

        if (isSnippet) {
            sj.add("class Test {");
        }

        sj.add(compact);

        if (isSnippet) {
            sj.add("}");
            sj.add("");
        }

        return sj.toString();
    }
}

/**
 * The name of the class that should be analyzed. Should only need to be provided when analyzing
 * inner classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TestClass {
    /**
     * The name of the class to inspect.
     *
     * @return the class name
     */
    String value() default "Test";
}
