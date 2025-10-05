// Keep somewhat in sync with
// ../defaultsPersist25/Driver.java and ../PersistUtil25.

import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.classfile.Annotation;
import java.lang.classfile.ClassModel;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Driver {

    private static final PrintStream out = System.out;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java Driver <HarnessClass>");
        }
        Object harness = Class.forName(args[0]).getDeclaredConstructor().newInstance();
        new Driver().runDriver(harness);
    }

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
                String fullSrc = PersistUtil25.wrap(compact);
                String testCls = PersistUtil25.testClassOf(m);

                ClassModel cm = PersistUtil25.compileAndReturn(fullSrc, testCls);

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

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ADescription {
    String annotation();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ADescriptions {
    ADescription[] value() default {};
}
