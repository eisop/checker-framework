// See https://github.com/eisop/checker-framework/issues/1862 .
// Real declarations of the overridden methods.
import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.List;

public class FOGenericReturnSuper {
    // Unannotated, so its return type is fully defaulted at both the primary and the
    // type-argument position: @Tainted List<@Tainted String>.
    public List<String> m() {
        return null;
    }

    // The type argument is explicitly @Untainted here. FOGenericReturnMid's presence-only fake
    // override of this method (below) must NOT inherit @Untainted at that position: a
    // presence-only fake override resets every position, including nested ones, to the
    // checker's default, not to what the overridden method declares.
    public List<@Untainted String> m2() {
        return null;
    }
}
