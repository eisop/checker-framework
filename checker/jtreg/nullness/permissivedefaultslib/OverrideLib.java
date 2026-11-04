package permissivedefaultslib;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class OverrideLib {
    public void unannotated(Object value) {}

    public void nullable(@Nullable Object value) {}

    public void nullableElement(List<@Nullable Object> values) {}

    public void nullableElementNarrowed(List<@Nullable Object> values) {}

    public void nullableComponent(@Nullable Object[] values) {}

    public void nullableComponentNarrowed(@Nullable Object[] values) {}

    public void unannotatedElement(List<String> values) {}

    public void unannotatedBound(List<? extends Object> values) {}

    public void nullableBoundNarrowed(List<? extends @Nullable Object> values) {}
}
