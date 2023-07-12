import org.checkerframework.checker.nullness.qual.PolyNull;

public class ReplacePolyNull {

    @PolyNull String foo(@PolyNull String param) {
        if (param != null) {
            //  @PolyNull is really @Nullable, so change
            // the type of param to @Nullable.
            return param;
        }
        if (param == null) {
            //  @PolyNull is really @Nullable, so change
            // the type of param to @Nullable.
            param = null;
            return null;
        }
        return param;
    }
}
