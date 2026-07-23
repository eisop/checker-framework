// See https://github.com/eisop/checker-framework/issues/1862 .
// Does not declare m() or m2() itself: FakeOverrideGenericReturn.astub fake-overrides both
// with generic return types, to test that AnnotationFileElementTypes#refreshFakeOverride
// threads an explicit type-argument annotation through (m()) and resets an unannotated
// type-argument position to the checker's default rather than inheriting it from the
// overridden method (m2()).
public class FOGenericReturnMid extends FOGenericReturnSuper {}
