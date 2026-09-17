// See https://github.com/eisop/checker-framework/issues/1862 .
// Does not declare m(int) itself: OrderFakeOverrideParam.astub fake-overrides it with a
// presence-only (unannotated) declaration.
public class OrderFOMidParam extends OrderFOSuperParam {}
