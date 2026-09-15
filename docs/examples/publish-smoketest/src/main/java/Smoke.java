// Trivial consumer of the published `checker` artifact. Compiling this
// (under Java 8 source/target, see ../build.gradle) is the actual
// regression test: it fails at dependency-resolution time, before javac
// even runs, if the published Gradle Module Metadata is broken -- see
// https://github.com/eisop/checker-framework/pull/1822.
import org.checkerframework.checker.nullness.qual.Nullable;

public class Smoke {
    public static @Nullable String greet() {
        return "ok";
    }

    public static void main(String[] args) {
        System.out.println(greet());
    }
}
