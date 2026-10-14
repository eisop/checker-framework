// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/Driver.java,
// ../defaultsPersist25/Driver.java,
// ../inheritDeclAnnoPersist/Driver.java, and
// ../PersistUtil.java.

// I removed some unnecessary code, e.g. declarations of @TA.
// I changed expected logic to handle multiple appearances
// of the same qualifier in different positions.

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.TypeAnnotation;
import com.sun.tools.classfile.TypeAnnotation.TargetType;

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
 * Test driver for verifying type annotations written into bytecode by the Nullness Checker, using
 * the legacy {@code com.sun.tools.classfile} API on JDK versions prior to 25.
 *
 * <p>For JDK 25 and later, see the counterpart driver {@code
 * checker/jtreg/nullness/defaultsPersist25/Driver.java} which uses the standard {@code
 * java.lang.classfile} API. For declaration annotation persistence testing, see {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist/Driver.java} (JDK &lt; 25) and {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist25/Driver.java} (JDK &gt;= 25).
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
            List<AnnoPosPair> expected = expectedOf(method);
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
                boolean ignoreConstructors = !clazz.getName().equals("Constructors");
                List<TypeAnnotation> actual =
                        ReferenceInfoUtil.extendedAnnotationsOf(cf, ignoreConstructors);
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
     * Extracts the expected type annotations declared on the given method.
     *
     * @param m the test method
     * @return the list of expected annotations with their target positions, or null if unannotated
     */
    private List<AnnoPosPair> expectedOf(Method m) {
        TADescription ta = m.getAnnotation(TADescription.class);
        TADescriptions tas = m.getAnnotation(TADescriptions.class);

        if (ta == null && tas == null) {
            return null;
        }

        List<AnnoPosPair> result = new ArrayList<>();

        if (ta != null) {
            result.add(expectedOf(ta));
        }

        if (tas != null) {
            for (TADescription a : tas.value()) {
                result.add(expectedOf(a));
            }
        }

        return result;
    }

    /**
     * Converts a {@link TADescription} annotation into an {@link AnnoPosPair}.
     *
     * @param d the description annotation
     * @return the annotation name and position pair
     */
    private AnnoPosPair expectedOf(TADescription d) {
        String annoName = d.annotation();

        TypeAnnotation.Position p = new TypeAnnotation.Position();
        p.type = TargetType.valueOf(d.type());
        if (d.offset() != NOT_SET) {
            p.offset = d.offset();
        }
        if (d.lvarOffset().length != 0) {
            p.lvarOffset = d.lvarOffset();
        }
        if (d.lvarLength().length != 0) {
            p.lvarLength = d.lvarLength();
        }
        if (d.lvarIndex().length != 0) {
            p.lvarIndex = d.lvarIndex();
        }
        if (d.boundIndex() != NOT_SET) {
            p.bound_index = d.boundIndex();
        }
        if (d.paramIndex() != NOT_SET) {
            p.parameter_index = d.paramIndex();
        }
        if (d.typeIndex() != NOT_SET) {
            p.type_index = d.typeIndex();
        }
        if (d.exceptionIndex() != NOT_SET) {
            p.exception_index = d.exceptionIndex();
        }
        if (d.genericLocation().length != 0) {
            p.location =
                    TypeAnnotation.Position.getTypePathFromBinary(
                            wrapIntArray(d.genericLocation()));
        }

        return AnnoPosPair.of(annoName, p);
    }

    /**
     * Wraps an array of primitive ints into a list of Integers.
     *
     * @param ints the array of integers
     * @return list of integers
     */
    private List<Integer> wrapIntArray(int[] ints) {
        List<Integer> list = new ArrayList<>(ints.length);
        for (int i : ints) {
            list.add(i);
        }
        return list;
    }

    /** Sentinel value indicating an unset field in {@link TADescription}. */
    public static final int NOT_SET = -888;
}

/** A pair of an annotation name and a position. */
class AnnoPosPair {
    /** The first element of the pair. */
    public final String first;

    /** The second element of the pair. */
    public final TypeAnnotation.Position second;

    /**
     * Creates a new immutable pair. Clients should use {@link #of}.
     *
     * @param first the first element of the pair
     * @param second the second element of the pair
     */
    private AnnoPosPair(String first, TypeAnnotation.Position second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Creates a new immutable pair.
     *
     * @param first first argument
     * @param second second argument
     * @return a pair of the values (first, second)
     */
    public static AnnoPosPair of(String first, TypeAnnotation.Position second) {
        return new AnnoPosPair(first, second);
    }
}

/**
 * Describes an expected type annotation in bytecode for a test method.
 *
 * <p>Test methods in the bytecode default persistence suites (such as {@code Classes}, {@code
 * Constructors}, {@code Fields}, {@code Methods}, {@code Extends}, and {@code Implements}) return
 * compact Java source snippets and are annotated with one or more {@code @TADescription}
 * annotations. Each annotation specifies the expected annotation type name, target type, target
 * info (offsets, indices, parameter bounds), and type path location in the compiled classfile as
 * specified by the Java Virtual Machine Specification (JVMS §4.7.20, "The {@code
 * RuntimeVisibleTypeAnnotations} attribute").
 *
 * <p>Both the JDK &lt; 25 driver ({@code checker/jtreg/nullness/defaultsPersist/Driver.java}) and
 * the JDK &gt;= 25 driver ({@code checker/jtreg/nullness/defaultsPersist25/Driver.java}) use this
 * identical annotation schema so test harnesses can share common test case definitions.
 *
 * @see TADescriptions
 * @see Driver
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescription {
    /**
     * The expected annotation type name (for example, {@code "NonNull"}, {@code "Nullable"}, or
     * fully qualified names).
     *
     * @return the annotation name
     */
    String annotation();

    /**
     * The target type of the type annotation, specified as the string name of a target type
     * constant (for example {@code "METHOD_RETURN"}, {@code "FIELD"}, {@code "LOCAL_VARIABLE"},
     * {@code "METHOD_FORMAL_PARAMETER"}, or {@code "CLASS_EXTENDS"}).
     *
     * <p>This is represented as a {@link String} rather than an enum so that the exact same test
     * case source code can be used across both the {@code
     * com.sun.tools.classfile.TypeAnnotation.TargetType} API (JDK &lt; 25) and the {@code
     * java.lang.classfile.TypeAnnotation.TargetType} API (JDK &gt;= 25).
     *
     * @return the target type name
     */
    String type();

    /**
     * The bytecode offset within the {@code Code} attribute for instruction-level targets, such as
     * {@code INSTANCEOF}, {@code NEW}, or method reference expressions (corresponding to {@code
     * offset_target} and {@code type_argument_target} in JVMS §4.7.20.1).
     *
     * @return the bytecode offset, or {@link Driver#NOT_SET} if not applicable
     */
    int offset() default Driver.NOT_SET;

    /**
     * The bytecode start PC offsets for local variable scopes (corresponding to {@code start_pc} in
     * {@code localvar_target.table[]} in JVMS §4.7.20.1).
     *
     * @return the array of local variable start PCs, or empty if not applicable
     */
    int[] lvarOffset() default {};

    /**
     * The lengths of bytecode instructions for local variable scopes (corresponding to {@code
     * length} in {@code localvar_target.table[]} in JVMS §4.7.20.1).
     *
     * @return the array of local variable scope lengths, or empty if not applicable
     */
    int[] lvarLength() default {};

    /**
     * The local variable table slot indices (corresponding to {@code index} in {@code
     * localvar_target.table[]} in JVMS §4.7.20.1).
     *
     * @return the array of local variable slot indices, or empty if not applicable
     */
    int[] lvarIndex() default {};

    /**
     * The 0-based bound index within the bounds of a type parameter declaration (corresponding to
     * {@code bound_index} in {@code type_parameter_bound_target} in JVMS §4.7.20.1).
     *
     * @return the bound index, or {@link Driver#NOT_SET} if not applicable
     */
    int boundIndex() default Driver.NOT_SET;

    /**
     * The 0-based parameter index for a method/constructor formal parameter (corresponding to
     * {@code formal_parameter_target.formal_parameter_index} in JVMS §4.7.20.1) or type parameter
     * index (corresponding to {@code type_parameter_target.type_parameter_index}).
     *
     * @return the parameter index, or {@link Driver#NOT_SET} if not applicable
     */
    int paramIndex() default Driver.NOT_SET;

    /**
     * The type index indicating the index into the {@code interfaces} array for {@code
     * CLASS_EXTENDS}, or the index into the throws exception table for {@code THROWS}, or the type
     * argument index for type arguments (JVMS §4.7.20.1).
     *
     * @return the type index, or {@link Driver#NOT_SET} if not applicable
     */
    int typeIndex() default Driver.NOT_SET;

    /**
     * The 0-based index into the {@code exception_table} array of the {@code Code} attribute
     * (corresponding to {@code catch_target.exception_table_index} in JVMS §4.7.20.1).
     *
     * @return the exception index, or {@link Driver#NOT_SET} if not applicable
     */
    int exceptionIndex() default Driver.NOT_SET;

    /**
     * The generic type path location pairs, encoded as a flat integer array {@code [type_path_kind,
     * type_argument_index, ...]} as specified in JVMS §4.7.20.2 ("The {@code type_path}
     * structure").
     *
     * <p>For example, {@code {0, 0}} steps into an array element type (kind 0), {@code {1, 0}}
     * steps into a nested type (kind 1), {@code {2, 0}} steps into a wildcard bound (kind 2), and
     * {@code {3, i}} steps into the <em>i</em>-th type argument of a parameterized type (kind 3).
     *
     * @return the flat array of type path entries, or empty if pointing directly to the root type
     */
    int[] genericLocation() default {};
}

/**
 * Container annotation for multiple {@link TADescription} annotations on a single test method.
 *
 * @see TADescription
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescriptions {
    /**
     * The array of {@link TADescription} annotations.
     *
     * @return the descriptions
     */
    TADescription[] value() default {};
}
