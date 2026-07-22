// See https://github.com/eisop/checker-framework/issues/1862 .
// Regression test: FOGenericReturnMid.m()'s fake override return type is
// List<@Untainted String>, not List<@Tainted String> (the checker's default, which is all
// that the primary annotation alone would carry). If AnnotationFileElementTypes#
// refreshFakeOverride only propagated the primary annotation and not the type argument,
// assigning the result to List<@Tainted String> would incorrectly type-check.
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.List;

public class FOGenericReturnUse {
    void test() {
        FOGenericReturnMid mid = new FOGenericReturnMid();
        List<@Untainted String> ok = mid.m();
        // :: error: (assignment.type.incompatible)
        List<@Tainted String> bad = mid.m();
    }
}
