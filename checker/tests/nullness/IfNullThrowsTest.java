// Test that @IfNullThrows (parameter-level postcondition: if null then throw) is respected by the
// Nullness Checker.

import org.checkerframework.checker.nullness.qual.IfNullThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

public class IfNullThrowsTest {

    // requireNonNull-style: if param is null, throws; so when returns, param is non-null
    public static <T> T requireNonNull(@IfNullThrows @Nullable T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    void useRequireNonNull(@Nullable String s) {
        requireNonNull(s);
        s.toString(); // OK: s refined to non-null after requireNonNull returns
    }

    // With message parameter
    public static <T> T requireNonNull(@IfNullThrows @Nullable T obj, String msg) {
        if (obj == null) {
            throw new NullPointerException(msg);
        }
        return obj;
    }

    void useRequireNonNullWithMsg(@Nullable String s) {
        requireNonNull(s, "s must not be null");
        s.toString(); // OK
    }

    // Multiple parameters with @IfNullThrows - validates both must be non-null to return
    public static void requireBothNonNull(
            @IfNullThrows @Nullable Object a, @IfNullThrows @Nullable Object b) {
        if (a == null || b == null) {
            throw new NullPointerException();
        }
    }

    void useRequireBothNonNull(@Nullable Object x, @Nullable Object y) {
        requireBothNonNull(x, y);
        x.toString(); // OK
        y.toString(); // OK
    }

    // Incorrect: has @IfNullThrows but never throws
    // :: error: (if.null.throws.must.throw)
    public static <T> T badNoThrow(@IfNullThrows @Nullable T obj) {
        return obj;
    }

    // Incorrect: has @IfNullThrows but only returns, no throw
    // :: error: (if.null.throws.must.throw)
    public static String badJustReturn(@IfNullThrows @Nullable String s) {
        return s == null ? "" : s;
    }
}
