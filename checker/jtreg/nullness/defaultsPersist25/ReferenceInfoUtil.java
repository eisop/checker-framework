import java.lang.classfile.AttributedElement;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;
import java.util.ArrayList;
import java.util.List;

public class ReferenceInfoUtil {

    public static final int IGNORE_VALUE = -321;
    private final boolean ignoreConstructors;

    private ReferenceInfoUtil(boolean ignoreConstructors) {
        this.ignoreConstructors = ignoreConstructors;
    }

    public static List<TypeAnnotation> extendedAnnotationsOf(ClassModel cm, boolean ignoreCtors) {
        ReferenceInfoUtil self = new ReferenceInfoUtil(ignoreCtors);
        List<TypeAnnotation> out = new ArrayList<>();
        self.collect(cm, out);
        return out;
    }

    private void collect(ClassModel cm, List<TypeAnnotation> sink) {
        addAnno(cm, sink);
        cm.fields().forEach(f -> addAnno(f, sink));

        for (MethodModel m : cm.methods()) {
            if (ignoreConstructors && m.methodName().stringValue().equals("<init>")) continue;

            addAnno(m, sink);
            m.findAttribute(Attributes.code()).ifPresent(code -> addAnno(code, sink));
        }
    }

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

    private static List<AnnoPosPair> dummy(List<Driver.AnnoTargetPair> src) {
        List<AnnoPosPair> r = new ArrayList<>(src.size());
        for (Driver.AnnoTargetPair p : src) r.add(AnnoPosPair.of(p.annoName, null));
        return r;
    }
}

class ComparisonException extends RuntimeException {
    final List<AnnoPosPair> expected;
    final List<TypeAnnotation> found;

    ComparisonException(String m, List<AnnoPosPair> e, List<TypeAnnotation> f) {
        super(m);
        expected = e;
        found = f;
    }

    public String toString() {
        return "%s%n  Expected(%d): %s%n  Found(%d): %s"
                .formatted(super.toString(), expected.size(), expected, found.size(), found);
    }
}

class AnnoPosPair {
    final String first;
    final Object second;

    private AnnoPosPair(String f, Object s) {
        first = f;
        second = s;
    }

    static AnnoPosPair of(String f, Object s) {
        return new AnnoPosPair(f, s);
    }
}
