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
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.StringJoiner;

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

        if (false) {
            try {
                File tmp = new File(System.getProperty("java.io.tmpdir"));
                File srcCopy = File.createTempFile("SrcCopy", ".java", tmp);
                File classCopy = File.createTempFile("ClassCopy", ".class", tmp);
                Files.copy(src.toPath(), srcCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(out.toPath(), classCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.printf("compileTestFile: copied to %s %s%n", srcCopy, classCopy);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
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
