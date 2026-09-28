import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A thrown object is never null (JLS 14.18: "throw null" throws a NullPointerException instead), so
 * a nullness annotation on a thrown type is always either a contradiction or redundant, and --
 * unlike an exception parameter, which is a reassignable local variable -- there is no legitimate
 * reason to write one.
 */
public class ThrowsType {
    void unannotated() throws Exception {}

    void nonNull()
            throws
                    // :: error: (nullness.on.throws)
                    @NonNull Exception {}

    void nullable()
            throws
                    // :: error: (nullness.on.throws)
                    @Nullable Exception {}
}
