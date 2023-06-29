import org.checkerframework.checker.nullness.qual.PolyNull;

class ConditionalPolyNull {
    public static @PolyNull String toLowerCaseA(@PolyNull String text) {
        return text == null ? null : text.toLowerCase();
    }

    public static @PolyNull String toLowerCaseB(@PolyNull String text) {
        return text != null ? text.toLowerCase() : null;
    }
}
