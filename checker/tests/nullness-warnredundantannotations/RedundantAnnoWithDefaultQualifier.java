import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(Nullable.class)
public class RedundantAnnoWithDefaultQualifier {

    // :: warning: (redundant.anno)
    void foo(@Nullable String message) {}

    // :: warning: (redundant.anno)
    @Nullable Integer foo(Integer i) {
        return i;
    }
}
