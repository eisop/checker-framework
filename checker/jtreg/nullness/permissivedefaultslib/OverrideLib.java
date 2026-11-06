package permissivedefaultslib;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OverrideLib {
    public void unannotated(Object value) {}

    public void nullable(@Nullable Object value) {}

    public void nullableElement(List<@Nullable Object> values) {}

    public void nullableElementNarrowed(List<@Nullable Object> values) {}

    public void nullableComponent(@Nullable Object[] values) {}

    public void nullableComponentNarrowed(@Nullable Object[] values) {}

    public void unannotatedElement(List<String> values) {}

    public void unannotatedElementWidened(List<String> values) {}

    public void unannotatedBound(List<? extends Object> values) {}

    public void nullableBoundNarrowed(List<? extends @Nullable Object> values) {}

    public Object unannotatedReturn() {
        return "";
    }

    public @NonNull Object nonNullReturn() {
        return "";
    }

    public List<@NonNull Object> nonNullElementReturn() {
        return Collections.emptyList();
    }

    public List<@NonNull Object> nonNullElementReturnChanged() {
        return Collections.emptyList();
    }

    public <T> void implicitBound(T value) {}

    public <T extends @Nullable Object> void nullableTypeParameterBound(T value) {}

    public <T extends @Nullable Object> void nullableTypeParameterBoundNarrowed(T value) {}

    public <T> void erasedBound(T value) {}

    public Map<String, Object> unannotatedElementSubclassReturn() {
        return new HashMap<>();
    }

    public Map<String, @NonNull Object> nonNullElementSubclassReturn() {
        return new HashMap<>();
    }
}
