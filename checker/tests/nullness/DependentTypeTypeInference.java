import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DependentTypeTypeInference {
    private final Map<String, Object> nameToPpt = new LinkedHashMap<>();

    public Collection<@KeyFor("nameToPpt") String> nameStringSet() {
        return Collections.unmodifiableSet(nameToPpt.keySet());
    }
}
