// See https://github.com/eisop/checker-framework/issues/1862 .
// Regression tests for AnnotationFileElementTypes#refreshFakeOverride's handling of a fake
// override's generic return type, exercising both directions of the bug this used to have:
// an explicit type-argument annotation must be threaded through (test()), and an
// unannotated position must reset to the checker's default rather than leak an unrelated
// annotation from the overridden method (test2()).
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.List;

public class FOGenericReturnUse {
    void test() {
        FOGenericReturnMid mid = new FOGenericReturnMid();
        // FOGenericReturnMid.m()'s fake override return type is List<@Untainted String>, not
        // List<@Tainted String> (the checker's default, which is all that the primary
        // annotation alone would carry).
        List<@Untainted String> ok = mid.m();
        // :: error: (assignment.type.incompatible)
        List<@Tainted String> bad = mid.m();
    }

    void test2() {
        FOGenericReturnMid mid = new FOGenericReturnMid();
        // FOGenericReturnMid.m2()'s fake override is entirely presence-only, so its return
        // type's type argument must reset to the checker's default (@Tainted), not inherit
        // @Untainted from FOGenericReturnSuper.m2(), the method it fake-overrides.
        List<@Tainted String> ok2 = mid.m2();
        // :: error: (assignment.type.incompatible)
        List<@Untainted String> bad2 = mid.m2();
    }
}
