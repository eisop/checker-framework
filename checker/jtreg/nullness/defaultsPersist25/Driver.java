// Keep somewhat in sync with
// ../defaultsPersist/Driver.java,
// ../inheritDeclAnnoPersist25/Driver.java, and
// ../PersistUtil.java.

import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.TargetInfo;
import java.lang.classfile.TypeAnnotation.TargetType;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Test driver for verifying type annotations written into bytecode by the Nullness Checker, using
 * the {@code java.lang.classfile} API available in JDK 25 and later.
 *
 * <p>For JDK versions prior to 25, see the counterpart driver {@code
 * checker/jtreg/nullness/defaultsPersist/Driver.java} which uses {@code com.sun.tools.classfile}.
 * For declaration annotation persistence testing, see {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist25/Driver.java} (JDK &gt;= 25) and {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist/Driver.java} (JDK &lt; 25).
 *
 * @see ReferenceInfoUtil
 * @see PersistUtil
 */
public class Driver {
    /** Sentinel value indicating an unset field in {@link TADescription}. */
    public static final int NOT_SET = -888;

    private static final PrintStream out = System.out;

    /**
     * Entry point to run test methods of the specified test class.
     *
     * @param a command-line arguments specifying the test class name
     * @throws Exception if reflection, compilation, or test execution fails
     */
    public static void main(String[] a) throws Exception {
        if (a.length != 1) throw new IllegalArgumentException("java Driver <HarnessClass>");
        Object h = Class.forName(a[0]).getDeclaredConstructor().newInstance();
        new Driver().runDriver(h);
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

        for (Method method : clazz.getMethods()) {
            List<AnnoTargetPair> expected = expectedOf(method);
            if (expected == null) continue;

            if (method.getReturnType() != String.class) {
                throw new IllegalArgumentException(
                        "Test method needs to return a string: " + method);
            }

            String testClass = PersistUtil.testClassOf(method);

            try {
                String compact = (String) method.invoke(harness);
                String fullFile = PersistUtil.wrap(compact);
                File clazzFile = PersistUtil.compile(fullFile, testClass);
                ClassModel cm = ClassFile.of().parse(clazzFile.toPath());

                boolean ignoreConstructors = !clazz.getName().equals("Constructors");
                List<TypeAnnotation> actual =
                        ReferenceInfoUtil.extendedAnnotationsOf(cm, ignoreConstructors);

                String diagnostic =
                        String.join(
                                "; ",
                                "Tests for " + clazz.getName(),
                                "compact=" + compact,
                                "fullFile=" + fullFile,
                                "testClass=" + testClass);

                ReferenceInfoUtil.compare(expected, actual, diagnostic);

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
     * @return the list of expected annotations with their targets and paths, or null if unannotated
     */
    private List<AnnoTargetPair> expectedOf(Method m) {
        TADescription one = m.getAnnotation(TADescription.class);
        TADescriptions many = m.getAnnotation(TADescriptions.class);
        if (one == null && many == null) return null;

        List<AnnoTargetPair> L = new ArrayList<>();
        if (one != null) L.add(toPair(one));
        if (many != null) for (TADescription d : many.value()) L.add(toPair(d));
        return L;
    }

    /**
     * Converts a {@link TADescription} annotation into an {@link AnnoTargetPair}.
     *
     * @param d the description annotation
     * @return the annotation name, target info, and type path
     */
    private AnnoTargetPair toPair(TADescription d) {
        TargetInfo t;
        switch (TargetType.valueOf(d.type())) {
            case FIELD -> t = TargetInfo.ofField();
            case METHOD_RETURN -> t = TargetInfo.ofMethodReturn();
            case METHOD_RECEIVER -> t = TargetInfo.ofMethodReceiver();
            case METHOD_FORMAL_PARAMETER -> t = TargetInfo.ofMethodFormalParameter(d.paramIndex());
            case THROWS -> t = TargetInfo.ofThrows(d.typeIndex());
            case CLASS_TYPE_PARAMETER -> t = TargetInfo.ofClassTypeParameter(d.paramIndex());
            case METHOD_TYPE_PARAMETER -> t = TargetInfo.ofMethodTypeParameter(d.paramIndex());
            case CLASS_TYPE_PARAMETER_BOUND ->
                    t = TargetInfo.ofClassTypeParameterBound(d.paramIndex(), d.boundIndex());
            case METHOD_TYPE_PARAMETER_BOUND ->
                    t = TargetInfo.ofMethodTypeParameterBound(d.paramIndex(), d.boundIndex());
            case LOCAL_VARIABLE -> t = TargetInfo.ofLocalVariable(List.of());
            default -> throw new UnsupportedOperationException("Unhandled " + d.type());
        }

        List<TypePathComponent> path = new ArrayList<>();
        int[] loc = d.genericLocation();
        for (int i = 0; i + 1 < loc.length; i += 2) {
            path.add(TypePathComponent.of(kindForTag(loc[i]), loc[i + 1]));
        }

        return AnnoTargetPair.of(d.annotation(), t, path);
    }

    /**
     * Returns the type-path component kind whose JVMS {@code type_path_kind} is {@code tag}.
     *
     * <p>The constants are looked up by {@link TypePathComponent.Kind#tag()} rather than by
     * ordinal. The two agree today, but the declaration order of an enum is not part of its
     * contract, whereas the tag is fixed by the class file format.
     *
     * @param tag a JVMS {@code type_path_kind} value
     * @return the corresponding kind
     */
    private static TypePathComponent.Kind kindForTag(int tag) {
        for (TypePathComponent.Kind k : TypePathComponent.Kind.values()) {
            if (k.tag() == tag) {
                return k;
            }
        }
        throw new IllegalArgumentException("no TypePathComponent.Kind has tag " + tag);
    }

    /** A tuple of an annotation name, target information, and type path components. */
    static final class AnnoTargetPair {
        /** The expected annotation name. */
        final String annoName;

        /** The target information. */
        final TargetInfo target;

        /** The type path component list. */
        final List<TypePathComponent> path;

        /**
         * Constructs an AnnoTargetPair.
         *
         * @param n the annotation name
         * @param t the target info
         * @param p the type path
         */
        private AnnoTargetPair(String n, TargetInfo t, List<TypePathComponent> p) {
            annoName = n;
            target = t;
            path = p;
        }

        /**
         * Factory method to create an {@link AnnoTargetPair}.
         *
         * @param n the annotation name
         * @param t the target info
         * @param p the type path
         * @return a new pair
         */
        static AnnoTargetPair of(String n, TargetInfo t, List<TypePathComponent> p) {
            return new AnnoTargetPair(n, t, p);
        }
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
