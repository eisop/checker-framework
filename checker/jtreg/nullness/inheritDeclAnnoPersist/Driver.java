// Keep somewhat in sync with
// ../inheritDeclAnnoPersist25/Driver.java,
// ../defaultsPersist/Driver.java, and
// ../PersistUtil.java.

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.ClassFile;

import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Test driver for verifying inherited declaration annotations written into bytecode by the Nullness
 * Checker, using the legacy {@code com.sun.tools.classfile} API on JDK versions prior to 25.
 *
 * <p>For JDK 25 and later, see the counterpart driver {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist25/Driver.java} which uses the standard {@code
 * java.lang.classfile} API. For type annotation persistence testing, see {@code
 * checker/jtreg/nullness/defaultsPersist/Driver.java} (JDK &lt; 25) and {@code
 * checker/jtreg/nullness/defaultsPersist25/Driver.java} (JDK &gt;= 25).
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
            throw new IllegalArgumentException("Usage: java Driver <test-name>");
        }
        String name = args[0];
        Class<?> clazz = Class.forName(name);
        new Driver().runDriver(clazz.newInstance());
    }

    /**
     * Runs all test methods defined on the given test instance.
     *
     * @param object the test suite instance
     * @throws Exception if test execution fails
     */
    protected void runDriver(Object object) throws Exception {
        int passed = 0, failed = 0;
        Class<?> clazz = object.getClass();
        out.println("Tests for " + clazz.getName());

        // Find methods
        for (Method method : clazz.getMethods()) {
            List<String> expected = expectedOf(method);
            if (expected == null) {
                continue;
            }
            if (method.getReturnType() != String.class) {
                throw new IllegalArgumentException(
                        "Test method needs to return a string: " + method);
            }
            String testClass = PersistUtil.testClassOf(method);

            try {
                String compact = (String) method.invoke(object);
                String fullFile = PersistUtil.wrap(compact);
                File clazzFile = PersistUtil.compile(fullFile, testClass);
                ClassFile cf = ClassFile.read(clazzFile);
                List<Annotation> actual = ReferenceInfoUtil.extendedAnnotationsOf(cf);
                String diagnostic =
                        String.join(
                                "; ",
                                "Tests for " + clazz.getName(),
                                "compact=" + compact,
                                "fullFile=" + fullFile,
                                "testClass=" + testClass);
                ReferenceInfoUtil.compare(expected, actual, cf, diagnostic);
                out.println("PASSED:  " + method.getName());
                ++passed;
            } catch (Throwable e) {
                out.println("FAILED:  " + method.getName());
                out.println("    " + e);
                ++failed;
            }
        }

        out.println();
        int total = passed + failed;
        out.println(total + " total tests: " + passed + " PASSED, " + failed + " FAILED");

        out.flush();

        if (failed != 0) {
            throw new RuntimeException(failed + " tests failed");
        }
    }

    /**
     * Extracts the expected declaration annotations declared on the given method.
     *
     * @param m the test method
     * @return the list of expected annotation class names, or null if unannotated
     */
    private List<String> expectedOf(Method m) {
        ADescription ta = m.getAnnotation(ADescription.class);
        ADescriptions tas = m.getAnnotation(ADescriptions.class);

        if (ta == null && tas == null) {
            return null;
        }

        List<String> result = new ArrayList<>();

        if (ta != null) {
            result.add(expectedOf(ta));
        }

        if (tas != null) {
            for (ADescription a : tas.value()) {
                result.add(expectedOf(a));
            }
        }

        return result;
    }

    /**
     * Returns the annotation class name from an {@link ADescription}.
     *
     * @param d the description annotation
     * @return the annotation class name
     */
    private String expectedOf(ADescription d) {
        return d.annotation();
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
