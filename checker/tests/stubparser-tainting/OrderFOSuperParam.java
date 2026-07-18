// See https://github.com/eisop/checker-framework/issues/1862 .
// This class's own parameter is annotated only via OrderFakeOverrideParam.astub, not in this
// source file. A fake override only ever touches a return type (see
// AnnotationFileParser#processFakeOverride), never a parameter, so
// OrderFOMidParam's presence-only fake override of m(int) below must inherit this
// parameter's stub-provided type unchanged -- regardless of where this class's own
// declaration sits within that stub file relative to the fake override.
public class OrderFOSuperParam {

    public void m(int p) {}
}
