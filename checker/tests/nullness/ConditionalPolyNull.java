// Test case for EISOP issue #519
// https://github.com/eisop/checker-framework/issues/519
import org.checkerframework.checker.nullness.qual.PolyNull;

class ConditionalPolyNull {
    @PolyNull String toLowerCaseA(@PolyNull String text) {
        return text == null ? null : text.toLowerCase();
    }

    @PolyNull String toLowerCaseB(@PolyNull String text) {
        return text != null ? text.toLowerCase() : null;
    }

    @PolyNull String toLowerCaseC(@PolyNull String text) {
        // :: error: (dereference.of.nullable)
        // :: error: (return.type.incompatible)
        return text == null ? text.toLowerCase() : null;
    }

    @PolyNull String toLowerCaseD(@PolyNull String text) {
        // :: error: (return.type.incompatible)
        // :: error: (dereference.of.nullable)
        return text != null ? null : text.toLowerCase();
    }

    @PolyNull String foo(@PolyNull String param) {
        if (param != null) {
            // @PolyNull is really @NonNull, so change
            // the type of param to @NonNull.
            return param.toString();
        }
        if (param == null) {
            // @PolyNull is really @Nullable, so change
            // the type of param to @Nullable.
            param = null;
            return null;
        }
        return param;
    }
}
