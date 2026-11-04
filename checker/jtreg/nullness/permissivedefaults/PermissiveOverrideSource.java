/*
 * @test
 *
 * @summary Test that a permissive default for a parameter of unchecked source code does not constrain an override.
 * @compile/fail/ref=PermissiveOverrideSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source PermissiveOverrideSource.java
 * @compile/fail/ref=PermissiveOverrideSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker PermissiveOverrideSource.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

// Not @AnnotatedFor("nullness"), so it is unchecked code.
class OverrideSourceLib {
    public void unannotated(Object value) {}

    public void nullable(@Nullable Object value) {}
}

@AnnotatedFor("nullness")
public class PermissiveOverrideSource extends OverrideSourceLib {
    // The permissive default makes the overridden parameter @Nullable, but it is not written, so
    // a non-null parameter is allowed.
    @Override
    public void unannotated(Object value) {}

    // The overridden parameter is @Nullable as written, so a non-null parameter is not allowed.
    @Override
    public void nullable(Object value) {}
}
