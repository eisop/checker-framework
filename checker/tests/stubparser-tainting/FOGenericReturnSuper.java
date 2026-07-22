// See https://github.com/eisop/checker-framework/issues/1862 .
// Real declaration of the overridden method. Unannotated, so its return type is fully
// defaulted at both the primary and the type-argument position: @Tainted List<@Tainted
// String>.
import java.util.List;

public class FOGenericReturnSuper {
    public List<String> m() {
        return null;
    }
}
