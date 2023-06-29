// Test case for EISOP issue #519
// https://github.com/eisop/checker-framework/issues/519
import org.checkerframework.checker.nullness.qual.PolyNull;

class ConditionalPolyNull {
    @PolyNull String toLowerCaseA(@PolyNull String text) {
        return text == null ? null : text.toLowerCase();
    }

    public static @PolyNull String toLowerCaseB(@PolyNull String text) {
        return text != null ? text.toLowerCase() : null;
    }
}
