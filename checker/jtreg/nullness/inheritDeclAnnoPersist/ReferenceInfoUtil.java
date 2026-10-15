// Keep somewhat in sync with
// langtools/test/tools/javac/annotations/typeAnnotations/referenceinfos/ReferenceInfoUtil.java
// Adapted to handle the same type qualifier appearing multiple times.

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPool.UnexpectedEntry;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.RuntimeAnnotations_attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Utility class for extracting and comparing declaration annotations from a {@link ClassFile} using
 * the legacy {@code com.sun.tools.classfile} API on JDK versions prior to 25.
 *
 * <p>For JDK 25 and later, see the counterpart utility {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist25/ReferenceInfoUtil.java} which uses {@code
 * java.lang.classfile}.
 *
 * @see Driver
 * @see PersistUtil
 */
public class ReferenceInfoUtil {

    /** Sentinel value for ignored attributes or indices. */
    public static final int IGNORE_VALUE = -321;

    /** Private constructor to prevent instantiation of utility class. */
    private ReferenceInfoUtil() {}

    /**
     * Extracts all declaration annotations from methods in the given class file.
     *
     * @param cf the class file to inspect
     * @return list of annotations found on methods
     */
    public static List<Annotation> extendedAnnotationsOf(ClassFile cf) {
        List<Annotation> annos = new ArrayList<>();
        findAnnotations(cf, annos);
        return annos;
    }

    // /////////////////// Extract annotations //////////////////
    private static void findAnnotations(ClassFile cf, List<Annotation> annos) {
        for (Method m : cf.methods) {
            findAnnotations(cf, m, Attribute.RuntimeVisibleAnnotations, annos);
        }
    }

    /**
     * Test the result of Attributes.getIndex according to expectations encoded in the method's
     * name.
     */
    private static void findAnnotations(
            ClassFile cf, Method m, String name, List<Annotation> annos) {
        int index = m.attributes.getIndex(cf.constant_pool, name);
        if (index != -1) {
            Attribute attr = m.attributes.get(index);
            assert attr instanceof RuntimeAnnotations_attribute;
            RuntimeAnnotations_attribute tAttr = (RuntimeAnnotations_attribute) attr;
            for (Annotation an : tAttr.annotations) {
                if (!containsName(annos, an, cf)) {
                    annos.add(an);
                }
            }
        }
    }

    private static Annotation findAnnotation(
            String name, List<Annotation> annotations, ClassFile cf)
            throws InvalidIndex, UnexpectedEntry {
        String properName = "L" + name + ";";
        for (Annotation anno : annotations) {
            String actualName = cf.constant_pool.getUTF8Value(anno.type_index);
            if (properName.equals(actualName)) {
                return anno;
            }
        }
        return null;
    }

    /**
     * Compares expected declaration annotations against actual annotations found on methods.
     *
     * @param expectedAnnos the expected annotation class names
     * @param actualAnnos the actual annotations found
     * @param cf the class file being inspected
     * @param diagnostic diagnostic context for error message
     * @return true if all expected annotations match
     * @throws InvalidIndex if constant pool index is invalid
     * @throws UnexpectedEntry if constant pool entry has unexpected type
     */
    public static boolean compare(
            List<String> expectedAnnos,
            List<Annotation> actualAnnos,
            ClassFile cf,
            String diagnostic)
            throws InvalidIndex, UnexpectedEntry {
        if (actualAnnos.size() != expectedAnnos.size()) {
            throw new ComparisonException(
                    "Wrong number of annotations; " + diagnostic, expectedAnnos, actualAnnos, cf);
        }
        for (String annoName : expectedAnnos) {
            Annotation anno = findAnnotation(annoName, actualAnnos, cf);
            if (anno == null) {
                throw new ComparisonException(
                        "Expected annotation not found: " + annoName + "; " + diagnostic,
                        expectedAnnos,
                        actualAnnos,
                        cf);
            }
        }
        return true;
    }

    private static boolean containsName(List<Annotation> annos, Annotation anno, ClassFile cf) {
        try {
            for (Annotation an : annos) {
                if (cf.constant_pool
                        .getUTF8Value(an.type_index)
                        .equals(cf.constant_pool.getUTF8Value(anno.type_index))) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
        return false;
    }
}

/**
 * Exception thrown when expected declaration annotations do not match actual annotations in
 * bytecode.
 */
class ComparisonException extends RuntimeException {
    private static final long serialVersionUID = -3930499712333815821L;

    /** The expected annotation names. */
    public final List<String> expected;

    /** The actual annotations found. */
    public final List<Annotation> found;

    /** The class file being inspected. */
    public final ClassFile cf;

    /**
     * Constructs a ComparisonException with diagnostic details.
     *
     * @param message the detail message
     * @param expected the expected annotation names
     * @param found the actual annotations found
     * @param cf the class file being inspected
     */
    public ComparisonException(
            String message, List<String> expected, List<Annotation> found, ClassFile cf) {
        super(message);
        this.expected = expected;
        this.found = found;
        this.cf = cf;
    }

    public String toString() {
        StringJoiner foundString = new StringJoiner(",");
        for (Annotation anno : found) {
            try {
                foundString.add(cf.constant_pool.getUTF8Value(anno.type_index));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return String.join(
                System.lineSeparator(),
                super.toString(),
                "\tExpected: "
                        + expected.size()
                        + " annotations; but found: "
                        + found.size()
                        + " annotations",
                "  Expected: " + expected,
                "  Found: " + foundString);
    }
}
