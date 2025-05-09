import java.io.PrintStream;
import java.lang.annotation.*;
import java.lang.classfile.*;
import java.lang.classfile.TypeAnnotation.TargetInfo;
import java.lang.classfile.TypeAnnotation.TargetType;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.reflect.Method;
import java.util.*;

public class Driver {
    public static final int NOT_SET = -888;
    private static final PrintStream out = System.out;

    public static void main(String[] a) throws Exception {
        if (a.length != 1) throw new IllegalArgumentException("java Driver <HarnessClass>");
        Object h = Class.forName(a[0]).getDeclaredConstructor().newInstance();
        new Driver().runDriver(h);
    }

    private void runDriver(Object h) throws Exception {
        int ok = 0, bad = 0;
        Class<?> C = h.getClass();
        out.println("Tests for " + C.getName());

        for (Method m : C.getMethods()) {
            List<AnnoTargetPair> exp = expectedOf(m);
            if (exp == null) continue;
            if (m.getReturnType() != String.class)
                throw new IllegalArgumentException("Method must return String: " + m);

            String compact = (String) m.invoke(h);
            String src = PersistUtil25.wrap(compact);
            ClassModel cm = PersistUtil25.compileAndReturn(src, PersistUtil25.testClassOf(m));

            boolean ignoreCtors = false;
            List<TypeAnnotation> act = ReferenceInfoUtil.extendedAnnotationsOf(cm, ignoreCtors);

            try {
                ReferenceInfoUtil.compare(exp, act, "method=" + m.getName());
                out.printf("PASSED:  %s%n", m.getName());
                ++ok;
            } catch (RuntimeException ex) {
                out.printf("FAILED:  %s — %s%n", m.getName(), ex.getMessage());
                ++bad;
            }
        }
        out.printf("%n%d total: %d PASSED, %d FAILED%n", ok + bad, ok, bad);
        if (bad != 0) throw new RuntimeException(bad + " tests failed");
    }

    private List<AnnoTargetPair> expectedOf(Method m) {
        TADescription one = m.getAnnotation(TADescription.class);
        TADescriptions many = m.getAnnotation(TADescriptions.class);
        if (one == null && many == null) return null;

        List<AnnoTargetPair> L = new ArrayList<>();
        if (one != null) L.add(toPair(one));
        if (many != null) for (TADescription d : many.value()) L.add(toPair(d));
        return L;
    }

    private AnnoTargetPair toPair(TADescription d) {
        TargetInfo t;
        switch (d.type()) {
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
        for (int i = 0; i + 1 < loc.length; i += 2)
            path.add(TypePathComponent.of(TypePathComponent.Kind.values()[loc[i]], loc[i + 1]));

        return AnnoTargetPair.of(d.annotation(), t, path);
    }

    /* tiny record */
    static final class AnnoTargetPair {
        final String annoName;
        final TargetInfo target;
        final List<TypePathComponent> path;

        private AnnoTargetPair(String n, TargetInfo t, List<TypePathComponent> p) {
            annoName = n;
            target = t;
            path = p;
        }

        static AnnoTargetPair of(String n, TargetInfo t, List<TypePathComponent> p) {
            return new AnnoTargetPair(n, t, p);
        }
    }
}

/* jtreg meta-annotations */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescription {
    String annotation();

    TargetType type();

    int offset() default Driver.NOT_SET;

    int[] lvarOffset() default {};

    int[] lvarLength() default {};

    int[] lvarIndex() default {};

    int boundIndex() default Driver.NOT_SET;

    int paramIndex() default Driver.NOT_SET;

    int typeIndex() default Driver.NOT_SET;

    int exceptionIndex() default Driver.NOT_SET;

    int[] genericLocation() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TADescriptions {
    TADescription[] value() default {};
}
