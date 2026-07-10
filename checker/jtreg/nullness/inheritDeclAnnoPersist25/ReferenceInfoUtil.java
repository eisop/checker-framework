import java.lang.classfile.*;
import java.lang.classfile.Annotation;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public final class ReferenceInfoUtil {

    public static final int IGNORE_VALUE = -321;

    public static List<Annotation> extendedAnnotationsOf(ClassModel cm) {
        List<Annotation> out = new ArrayList<>();
        for (MethodModel m : cm.methods()) {
            addAnnotations(m, out, false);
        }
        return out;
    }

    private static void addAnnotations(MethodModel m, List<Annotation> sink, boolean allowDup) {
        m.findAttribute(Attributes.runtimeVisibleAnnotations())
                .ifPresent(attr -> attr.annotations().forEach(a -> addIfUnique(a, sink, allowDup)));

        m.findAttribute(Attributes.runtimeInvisibleAnnotations())
                .ifPresent(attr -> attr.annotations().forEach(a -> addIfUnique(a, sink, allowDup)));
    }

    private static void addIfUnique(Annotation a, List<Annotation> sink, boolean allowDup) {
        if (allowDup || !containsByType(sink, a)) sink.add(a);
    }

    private static boolean containsByType(List<Annotation> pool, Annotation cand) {
        String desc = cand.className().stringValue();
        for (Annotation a : pool) {
            if (desc.equals(a.className().stringValue())) return true;
        }
        return false;
    }

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

    private static Annotation findAnnotation(String binaryName, List<Annotation> pool) {
        String desc = 'L' + binaryName + ';';
        for (Annotation a : pool) {
            if (desc.equals(a.className().stringValue())) return a;
        }
        return null;
    }
}

class ComparisonException extends RuntimeException {
    private static final long serialVersionUID = -3930499712333815821L;

    final List<String> expected;
    final List<Annotation> found;

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
