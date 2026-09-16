// Keep somewhat in sync with
// ../inheritDeclAnnoPersist/Driver.java,
// ../defaultsPersist25/Driver.java, and
// ../PersistUtil.java.

import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.classfile.Annotation;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Test driver for verifying inherited declaration annotations written into bytecode by the Nullness
 * Checker, using the {@code java.lang.classfile} API available in JDK 25 and later.
 *
 * <p>For JDK versions prior to 25, see the counterpart driver {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist/Driver.java} which uses {@code
 * com.sun.tools.classfile}. For type annotation persistence testing, see {@code
 * checker/jtreg/nullness/defaultsPersist25/Driver.java} (JDK &gt;= 25) and {@code
 * checker/jtreg/nullness/defaultsPersist/Driver.java} (JDK &lt; 25).
 *
 * @see ReferenceInfoUtil
 * @see PersistUtil
 */
public class Driver {

    private static final PrintStream out = System.out;

    /**
     * Entry point to run test methods of the specified test class.
     *
     * @param args command-line arguments specifying the test class name
     * @throws Exception if reflection, compilation, or test execution fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java Driver <HarnessClass>");
        }
        Object harness = Class.forName(args[0]).getDeclaredConstructor().newInstance();
        new Driver().runDriver(harness);
    }

    /**
     * Runs all test methods defined on the given test instance.
     *
     * @param harness the test suite instance
     * @throws Exception if test execution fails
     */
    private void runDriver(Object harness) throws Exception {
        int passed = 0, failed = 0;
        Class<?> clazz = harness.getClass();
        out.println("Tests for " + clazz.getName());

        for (Method m : clazz.getMethods()) {
            List<String> expected = expectedOf(m);
            if (expected == null) continue;

            if (m.getReturnType() != String.class) {
                throw new IllegalArgumentException("Test method must return string: " + m);
            }

            try {
                String compact = (String) m.invoke(harness);
                String fullSrc = PersistUtil.wrap(compact);
                String testCls = PersistUtil.testClassOf(m);

                File clazzFile = PersistUtil.compile(fullSrc, testCls);
                ClassModel cm = ClassFile.of().parse(clazzFile.toPath());

                List<Annotation> actual = ReferenceInfoUtil.extendedAnnotationsOf(cm);

                String diag =
                        String.join(
                                "; ",
                                "method=" + m.getName(),
                                "compact=" + compact,
                                "testClass=" + testCls);

                ReferenceInfoUtil.compare(expected, actual, diag);

                out.printf("PASSED:  %s%n", m.getName());
                ++passed;
            } catch (Throwable ex) {
                out.printf("FAILED:  %s — %s%n", m.getName(), ex.getMessage());
                ++failed;
            }
        }

        out.printf("%n%d total: %d PASSED, %d FAILED%n", passed + failed, passed, failed);
        if (failed != 0) throw new RuntimeException(failed + " tests failed");
    }

    /**
     * Extracts the expected declaration annotations declared on the given method.
     *
     * @param m the test method
     * @return the list of expected annotation class names, or null if unannotated
     */
    private List<String> expectedOf(Method m) {
        ADescription one = m.getAnnotation(ADescription.class);
        ADescriptions many = m.getAnnotation(ADescriptions.class);

        if (one == null && many == null) return null;

        List<String> L = new ArrayList<>();
        if (one != null) L.add(one.annotation());
        if (many != null) for (ADescription d : many.value()) L.add(d.annotation());
        return L;
    }
}

/**
 * Describes an expected declaration annotation in bytecode for a test method.
 *
 * <p>Test methods in the inheritance test suites (such as {@code Classes}, {@code Methods}, {@code
 * Fields}, and {@code Constructors}) return test source code snippets and are annotated with one or
 * more {@code @ADescription} annotations specifying the declaration annotations expected in the
 * compiled bytecode.
 *
 * @see ADescriptions
 * @see Driver
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ADescription {
    /**
     * The expected annotation type name (either a simple name like {@code "NonNull"} or a fully
     * qualified name).
     *
     * @return the annotation name
     */
    String annotation();
}

/**
 * Container annotation for multiple {@link ADescription} annotations on a single test method.
 *
 * @see ADescription
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ADescriptions {
    /**
     * The array of {@link ADescription} annotations.
     *
     * @return the descriptions
     */
    ADescription[] value() default {};
}
