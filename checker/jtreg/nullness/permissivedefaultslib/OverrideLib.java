package permissivedefaultslib;

import org.checkerframework.checker.nullness.qual.Nullable;

public class OverrideLib {
    public void unannotated(Object value) {}

    public void nullable(@Nullable Object value) {}
}
