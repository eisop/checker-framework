// Keep somewhat in sync with
// ../inheritDeclAnnoPersist/ReferenceInfoUtil.java and ../PersistUtil.java.

import java.lang.classfile.*;
import java.lang.classfile.Annotation;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Utility class for extracting and comparing declaration annotations from a {@link ClassModel}
 * using the {@code java.lang.classfile} API available in JDK 25 and later.
 *
 * <p>For JDK versions prior to 25, see the counterpart utility {@code
 * checker/jtreg/nullness/inheritDeclAnnoPersist/ReferenceInfoUtil.java} which uses {@code
 * com.sun.tools.classfile}.
 *
 * @see Driver
 * @see PersistUtil
 */
public final class ReferenceInfoUtil {

    /** Sentinel value for ignored attributes or indices. */
    public static final int IGNORE_VALUE = -321;

    /** Private constructor to prevent instantiation of utility class. */
    private ReferenceInfoUtil() {}

    /**
     * Extracts all declaration annotations from methods in the given class model.
     *
     * @param cm the class model to inspect
     * @return list of annotations found on methods
     */
    public static List<Annotation> extendedAnnotationsOf(ClassModel cm) {
        List<Annotation> out = new ArrayList<>();
        for (MethodModel m : cm.methods()) {
            addAnnotations(m, out, false);
        }
        return out;
    }

    /**
     * Extracts runtime visible and invisible annotations from a method model into the sink.
     *
     * @param m the method model to inspect
     * @param sink the collection to append annotations to
     * @param allowDup whether duplicate annotations by type are permitted
     */
    private static void addAnnotations(MethodModel m, List<Annotation> sink, boolean allowDup) {
        m.findAttribute(Attributes.runtimeVisibleAnnotations())
                .ifPresent(attr -> attr.annotations().forEach(a -> addIfUnique(a, sink, allowDup)));

        m.findAttribute(Attributes.runtimeInvisibleAnnotations())
                .ifPresent(attr -> attr.annotations().forEach(a -> addIfUnique(a, sink, allowDup)));
    }

    /**
     * Adds the annotation to the sink if duplicates are allowed or none of the same type exists.
     *
     * @param a the annotation to add
     * @param sink the collection to append to
     * @param allowDup whether duplicates are allowed
     */
    private static void addIfUnique(Annotation a, List<Annotation> sink, boolean allowDup) {
        if (allowDup || !containsByType(sink, a)) sink.add(a);
    }

    /**
     * Returns true if the pool contains an annotation with the same class name descriptor as the
     * candidate.
     *
     * @param pool the collection of annotations
     * @param cand the candidate annotation
     * @return true if a match is found, false otherwise
     */
    private static boolean containsByType(List<Annotation> pool, Annotation cand) {
        String desc = cand.className().stringValue();
        for (Annotation a : pool) {
            if (desc.equals(a.className().stringValue())) return true;
        }
        return false;
    }

    /**
     * Compares expected declaration annotations against actual annotations found on methods.
     *
     * @param expected the expected annotation class names
     * @param actual the actual annotations found
     * @param diagnostic diagnostic context for error message
     * @return true if all expected annotations match
     * @throws ComparisonException if counts differ or an expected annotation is missing
     */
    public static boolean compare(
            List<String> expected, List<Annotation> actual, String diagnostic) {

        if (actual.size() != expected.size()) {
            throw new ComparisonException("wrong count — " + diagnostic, expected, actual);
        }
        for (String name : expected) {
            if (findAnnotation(name, actual) == null) {
                throw new ComparisonException(
                        "missing " + name + " — " + diagnostic, expected, actual);
            }
        }
        return true;
    }

    /**
     * Finds an annotation in the pool matching the given binary name descriptor.
     *
     * @param binaryName the binary name of the annotation class
     * @param pool the annotations to search
     * @return matching annotation, or null if not found
     */
    private static Annotation findAnnotation(String binaryName, List<Annotation> pool) {
        String desc = 'L' + binaryName + ';';
        for (Annotation a : pool) {
            if (desc.equals(a.className().stringValue())) return a;
        }
        return null;
    }
}

/** Exception thrown when expected declaration annotations do not match actual annotations. */
class ComparisonException extends RuntimeException {
    private static final long serialVersionUID = -3930499712333815821L;

    /** The expected annotation names. */
    final List<String> expected;

    /** The actual annotations found. */
    final List<Annotation> found;

    /**
     * Constructs a ComparisonException with details.
     *
     * @param msg the error message
     * @param expected expected annotation names
     * @param found found annotations
     */
    ComparisonException(String msg, List<String> expected, List<Annotation> found) {
        super(msg);
        this.expected = expected;
        this.found = found;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (Annotation a : found) sj.add(a.className().stringValue());
        return String.join(
                System.lineSeparator(),
                super.toString(),
                "  Expected(" + expected.size() + "): " + expected,
                "  Found(" + found.size() + "):    " + sj);
    }
}
