// Trivial consumer of the published `checker` artifact. Compiling this
// (under Java 8 source/target, see ../build.gradle) with the Nullness Checker
// fails at dependency-resolution time if the published metadata is broken, and
// when the checker is loaded if its jar lacks a class that it needs.
import org.checkerframework.checker.nullness.qual.Nullable;

public class Smoke {
    public static @Nullable String greet() {
        return "ok";
    }

    public static void main(String[] args) {
        System.out.println(greet());
    }
}
