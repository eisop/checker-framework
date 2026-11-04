// Compiled with the published `framework-all` artifact on the annotation processor path,
// running a checker out of it (see ../../../build.gradle). Merely *resolving* framework-all
// is not enough to show that it works: the artifact is advertised as self-contained, and a
// missing bundled dependency only surfaces when its classes are actually loaded. Running a
// real checker loads SourceChecker, GenericAnnotatedTypeFactory and the dataflow machinery,
// which is what caught framework-all shipping without Guava.
public class FrameworkAllSmoke {
    public static int len(String s) {
        return s.length();
    }

    public static void main(String[] args) {
        System.out.println(len("ok"));
    }
}
