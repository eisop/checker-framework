import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * The {@code java.lang.classfile} counterpart of {@link PersistUtil}, for JDK 25 and later, where
 * {@code com.sun.tools.classfile} no longer exists. It compiles a test snippet and returns the
 * result as a {@link ClassModel} rather than a {@code ClassFile}; everything else about the
 * workflow is the same, including that {@code -processor
 * org.checkerframework.checker.nullness.NullnessChecker} is added to the compiler invocation here
 * rather than by the caller.
 *
 * <p>Each test is selected for exactly one of the two harnesses by a {@code @requires
 * jdk.version.major} guard, so the two never run against the same JDK.
 */
final class PersistUtil25 {

    static String testClassOf(Method m) {
        TestClass tc = m.getAnnotation(TestClass.class);
        return (tc != null) ? tc.value() : "Test";
    }

    static ClassModel compileAndReturn(String fullSource, String testClass) throws Exception {
        File source = writeTestFile(fullSource);
        File classFile = compileTestFile(source, testClass);
        return ClassFile.of().parse(classFile.toPath());
    }

    private static File writeTestFile(String fullSource) throws IOException {
        File f = new File("Test.java");
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
            out.println(fullSource);
        }
        return f;
    }

    private static File compileTestFile(File src, String testClass) {
        int rc =
                com.sun.tools.javac.Main.compile(
                        new String[] {
                            "-AnoJreVersionCheck",
                            "-g",
                            "-processor",
                            "org.checkerframework.checker.nullness.NullnessChecker",
                            src.getPath()
                        });
        if (rc != 0) throw new Error("compilation failed, rc=" + rc);

        File out = new File(src.getParent(), testClass + ".class");

        return out;
    }

    static String wrap(String compact) {
        StringJoiner sj = new StringJoiner(System.lineSeparator());

        sj.add("");
        sj.add("import java.util.*;");
        sj.add("import java.lang.annotation.*;");
        sj.add("import org.checkerframework.framework.qual.DefaultQualifier;");
        sj.add("import org.checkerframework.checker.nullness.qual.*;");
        sj.add("import org.checkerframework.dataflow.qual.*;");
        sj.add("");

        boolean snippet =
                !(compact.startsWith("class") || compact.contains(" class"))
                        && !compact.contains("interface")
                        && !compact.contains("enum");

        if (snippet) sj.add("class Test {");
        sj.add(compact);
        if (snippet) {
            sj.add("}");
            sj.add("");
        }

        return sj.toString();
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TestClass {
    String value() default "Test";
}
