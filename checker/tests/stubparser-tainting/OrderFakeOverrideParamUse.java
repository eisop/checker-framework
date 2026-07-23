// See https://github.com/eisop/checker-framework/issues/1862 .
// Regression test: a fake override's stored snapshot must not go stale just because the
// overridden method's own stub-provided parameter annotation is declared later in the same
// stub file (OrderFakeOverrideParam.astub). A fake override never touches parameter types
// itself, so OrderFOMidParam.m(int) must require exactly what OrderFOSuperParam.m(int)
// requires.
import org.checkerframework.checker.tainting.qual.Tainted;

public class OrderFakeOverrideParamUse {
    void m(@Tainted int t) {
        OrderFOSuperParam sup = new OrderFOSuperParam();
        OrderFOMidParam mid = new OrderFOMidParam();

        // :: error: (argument.type.incompatible)
        sup.m(t);
        // :: error: (argument.type.incompatible)
        mid.m(t);
    }
}
