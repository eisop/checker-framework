import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.framework.qual.HasQualifierParameter;
import org.checkerframework.framework.qual.NoQualifierParameter;

@HasQualifierParameter(Nullable.class)
public class TestPolyNullOnField {
    @PolyNull String field;

    @PolyNull("test") String field2;

    @NoQualifierParameter(Nullable.class)
    class Inner {
        // :: error: (invalid.polymorphic.qualifier.use)
        @PolyNull String field;

        // :: error: (invalid.polymorphic.qualifier.use)
        @PolyNull("test") String field2;
    }
}
