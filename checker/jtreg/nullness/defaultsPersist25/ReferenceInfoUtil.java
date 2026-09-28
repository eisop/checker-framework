// Keep somewhat in sync with
// ../defaultsPersist/ReferenceInfoUtil.java and ../PersistUtil.java.

import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for extracting and comparing type annotations from a {@link ClassModel} using the
 * {@code java.lang.classfile} API available in JDK 25 and later.
 *
 * <p>For JDK versions prior to 25, see the counterpart utility {@code
 * checker/jtreg/nullness/defaultsPersist/ReferenceInfoUtil.java} which uses {@code
 * com.sun.tools.classfile}.
 *
 * @see Driver
 * @see PersistUtil
 */
public class ReferenceInfoUtil {

    /** Sentinel value for ignored attributes or indices. */
    public static final int IGNORE_VALUE = -321;

    /** Whether to ignore annotations on constructor methods. */
    private final boolean ignoreConstructors;

    /**
     * Constructs a utility instance with constructor filtering configuration.
     *
     * @param ignoreConstructors if true, constructors are ignored
     */
    private ReferenceInfoUtil(boolean ignoreConstructors) {
        this.ignoreConstructors = ignoreConstructors;
    }

    /**
     * Extracts all type annotations from the given class model.
     *
     * @param cm the class model to inspect
     * @param ignoreCtors whether to ignore constructor methods
     * @return list of type annotations found
     */
    public static List<TypeAnnotation> extendedAnnotationsOf(ClassModel cm, boolean ignoreCtors) {
        ReferenceInfoUtil self = new ReferenceInfoUtil(ignoreCtors);
        List<TypeAnnotation> out = new ArrayList<>();
        self.collect(cm, out);
        return out;
    }

    /**
     * Collects type annotations from class, fields, methods, and code attributes into the sink.
     *
     * @param cm the class model to inspect
     * @param sink the collection to append annotations to
     */
    private void collect(ClassModel cm, List<TypeAnnotation> sink) {
        addAnno(cm, sink);
        cm.fields().forEach(f -> addAnno(f, sink));

        for (MethodModel m : cm.methods()) {
            if (ignoreConstructors && m.methodName().stringValue().equals("<init>")) continue;

            addAnno(m, sink);
            m.findAttribute(Attributes.code()).ifPresent(code -> addAnno(code, sink));
        }
    }

    /**
     * Extracts runtime visible and invisible type annotations from an attributed element.
     *
     * @param elt the attributed element
     * @param sink the collection to append annotations to
     */
    private static void addAnno(AttributedElement elt, List<TypeAnnotation> sink) {
        elt.findAttribute(Attributes.runtimeVisibleTypeAnnotations())
                .ifPresent(a -> sink.addAll(a.annotations()));
        elt.findAttribute(Attributes.runtimeInvisibleTypeAnnotations())
                .ifPresent(a -> sink.addAll(a.annotations()));
    }

    /**
     * Checks that {@code actual} contains exactly the annotations {@code expect} describes, with no
     * extras and no expectation matched twice.
     *
     * <p>Each match is removed from the working copy of {@code actual}, so two identical
     * expectations require two identical annotations. Comparing only sizes and then asking whether
     * each expectation occurs somewhere would accept {@code [A, B]} for {@code [A, A]}: the two
     * lookups of {@code A} would both find the same annotation, and {@code B} would never be
     * examined.
     *
     * @param expect the annotations the test declares, from its {@code @TADescription}s
     * @param actual the annotations read from the compiled class
     * @param where a description of the test, for the failure message
     * @return true if they correspond; never returns false, since a mismatch throws
     */
    public static boolean compare(
            List<Driver.AnnoTargetPair> expect, List<TypeAnnotation> actual, String where) {

        List<TypeAnnotation> unmatched = new ArrayList<>(actual);
        for (Driver.AnnoTargetPair e : expect) {
            TypeAnnotation found = find(e, unmatched);
            if (found == null) {
                throw new ComparisonException(
                        "expected but not found: " + e.annoName + " @" + where,
                        dummy(expect),
                        actual);
            }
            unmatched.remove(found);
        }
        if (!unmatched.isEmpty()) {
            throw new ComparisonException(
                    unmatched.size() + " unexpected annotation(s) @" + where,
                    dummy(expect),
                    actual);
        }
        return true;
    }

    /**
     * Returns an annotation in {@code pool} that {@code want} describes, or null if there is none.
     *
     * @param want the expectation to satisfy
     * @param pool the annotations still unmatched
     * @return a matching annotation, or null
     */
    private static TypeAnnotation find(Driver.AnnoTargetPair want, List<TypeAnnotation> pool) {

        String desc = "L" + want.annoName + ";";
        for (TypeAnnotation ta : pool) {
            if (!ta.annotation().className().stringValue().equals(desc)) continue;
            if (!ta.targetInfo().equals(want.target)) continue;
            if (!ta.targetPath().equals(want.path)) continue;
            return ta;
        }
        return null;
    }

    /**
     * Converts expected annotation pairs into placeholder pairs for error reporting.
     *
     * @param src the expected annotation pairs
     * @return list of placeholder pairs
     */
    private static List<AnnoPosPair> dummy(List<Driver.AnnoTargetPair> src) {
        List<AnnoPosPair> r = new ArrayList<>(src.size());
        for (Driver.AnnoTargetPair p : src) r.add(AnnoPosPair.of(p.annoName, null));
        return r;
    }
}

/** Exception thrown when expected annotations do not match actual annotations found in bytecode. */
class ComparisonException extends RuntimeException {
    /** The list of expected annotations. */
    final List<AnnoPosPair> expected;

    /** The list of actual annotations found. */
    final List<TypeAnnotation> found;

    /**
     * Constructs a ComparisonException with diagnostic details.
     *
     * @param m the error message
     * @param e the expected annotations
     * @param f the found annotations
     */
    ComparisonException(String m, List<AnnoPosPair> e, List<TypeAnnotation> f) {
        super(m);
        expected = e;
        found = f;
    }

    @Override
    public String toString() {
        return "%s%n  Expected(%d): %s%n  Found(%d): %s"
                .formatted(super.toString(), expected.size(), expected, found.size(), found);
    }
}

/** Represents an annotation and its position or target details for comparison reporting. */
class AnnoPosPair {
    /** The annotation name. */
    final String first;

    /** The target or position information. */
    final Object second;

    /**
     * Constructs an annotation-position pair.
     *
     * @param f the annotation name
     * @param s the target or position information
     */
    private AnnoPosPair(String f, Object s) {
        first = f;
        second = s;
    }

    /**
     * Factory method to create an annotation-position pair.
     *
     * @param f the annotation name
     * @param s the target or position information
     * @return a new pair
     */
    static AnnoPosPair of(String f, Object s) {
        return new AnnoPosPair(f, s);
    }
}
