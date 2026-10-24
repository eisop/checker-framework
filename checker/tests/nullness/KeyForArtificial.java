import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Enhanced-for loops create artificial trees in the CFG (the iterator variable and the calls of
// iterator(), hasNext(), and next()). The @KeyFor expressions in their types name local variables
// and must be resolved in the scope of the loop.
public class KeyForArtificial {

    void enhancedFor(Map<String, Integer> param) {
        Map<String, Integer> map = new HashMap<>();
        for (@KeyFor("map") String k : map.keySet()) {
            @NonNull Integer v = map.get(k);
        }
        List<@KeyFor("map") String> keys = new java.util.ArrayList<>(map.keySet());
        for (String k : keys) {
            @NonNull Integer v = map.get(k);
        }
        for (@KeyFor("param") String k : param.keySet()) {
            @NonNull Integer v = param.get(k);
        }
    }

    void array(Map<String, Integer> param) {
        Map<String, Integer> map = new HashMap<>();
        @KeyFor("map") String[] keys = map.keySet().toArray(new @KeyFor("map") String[0]);
        for (String k : keys) {
            @NonNull Integer v = map.get(k);
        }
    }

    void lambda() {
        Map<String, Integer> map = new HashMap<>();
        Runnable r =
                () -> {
                    for (@KeyFor("map") String k : map.keySet()) {
                        @NonNull Integer v = map.get(k);
                    }
                };
    }
}
