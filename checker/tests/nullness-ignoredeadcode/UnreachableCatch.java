// Same shape as unreachableCatch() in checker/tests/nullness-initialization/TryCatch.java, but
// checked with -AignoreDeadCode: the errors reported there disappear here because the try body
// cannot throw anything, so the catch block is unreachable.
public class UnreachableCatch {
    void unreachableCatch() {
        String t = "";
        t.toString();
        try {
        } catch (Throwable e) {
            // Note that this code is dead; no error expected under -AignoreDeadCode.
            t.toString();
        }
    }
}
