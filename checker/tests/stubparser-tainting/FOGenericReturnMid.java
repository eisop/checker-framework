// See https://github.com/eisop/checker-framework/issues/1862 .
// Does not declare m() itself: FakeOverrideGenericReturn.astub fake-overrides it with a
// return type that annotates a type ARGUMENT (not just the primary annotation on the
// outermost type), to test that AnnotationFileElementTypes#refreshFakeOverride threads the
// whole return-type structure, not only its primary annotation.
public class FOGenericReturnMid extends FOGenericReturnSuper {}
