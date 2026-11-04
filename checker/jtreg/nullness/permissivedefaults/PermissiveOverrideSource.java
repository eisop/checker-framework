/*
 * @test
 *
 * @summary Test that a permissive default in the signature of a method of unchecked source code (a parameter, the return type, or a type parameter bound) does not constrain an override, unlike a written qualifier, whichever class is checked first.
 * @compile/fail/ref=PermissiveOverrideSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source ../permissivedefaultslib/OverrideLib.java PermissiveOverrideSource.java
 * @compile/fail/ref=PermissiveOverrideSource.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source PermissiveOverrideSource.java ../permissivedefaultslib/OverrideLib.java
 * @compile/fail/ref=PermissiveOverrideSourceNoOption.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker ../permissivedefaultslib/OverrideLib.java PermissiveOverrideSource.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

import java.util.Collections;
import java.util.List;

import permissivedefaultslib.OverrideLib;

// OverrideLib is not @AnnotatedFor("nullness"), so it is unchecked code.
@AnnotatedFor("nullness")
public class PermissiveOverrideSource extends OverrideLib {
    // Only the permissive default makes the overridden parameter @Nullable, so a non-null
    // parameter is allowed.
    @Override
    public void unannotated(Object value) {}

    // The overridden parameter is @Nullable as written, so a non-null parameter is not allowed.
    @Override
    public void nullable(Object value) {}

    // The overridden List has no written qualifier, but its element type is written @Nullable.
    @Override
    public void nullableElement(List<@Nullable Object> values) {}

    @Override
    public void nullableElementNarrowed(List<Object> values) {}

    // The overridden array has no written qualifier, but its component type is written @Nullable.
    @Override
    public void nullableComponent(@Nullable Object[] values) {}

    @Override
    public void nullableComponentNarrowed(Object[] values) {}

    // No qualifier is written on the overridden List or its element type.
    @Override
    public void unannotatedElement(List<String> values) {}

    // No qualifier is written on the bound of the overridden wildcard.
    @Override
    public void unannotatedBound(List<? extends Object> values) {}

    // The bound of the overridden wildcard is written @Nullable.
    @Override
    public void nullableBoundNarrowed(List<? extends Object> values) {}

    // Only the permissive default makes the overridden return type @NonNull.
    @Override
    public @Nullable Object unannotatedReturn() {
        return null;
    }

    // The overridden return type is @NonNull as written.
    @Override
    public @Nullable Object nonNullReturn() {
        return null;
    }

    // The overridden List has no written qualifier, but its element type is written @NonNull.
    @Override
    public @Nullable List<@NonNull Object> nonNullElementReturn() {
        return null;
    }

    @Override
    public List<@Nullable Object> nonNullElementReturnChanged() {
        return Collections.emptyList();
    }

    // No qualifier is written on the bounds of the overridden type parameter.
    @Override
    public <T> void implicitBound(T value) {}

    // The upper bound of the overridden type parameter is written @Nullable.
    @Override
    public <T extends @Nullable Object> void nullableTypeParameterBound(T value) {}

    @Override
    public <T extends @NonNull Object> void nullableTypeParameterBoundNarrowed(T value) {}
}
