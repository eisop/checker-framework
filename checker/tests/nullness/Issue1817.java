// Test case for eisop/checker-framework PR #1817, which exposed invalid sharing in CopyOnWriteMaps.
// Based on a CI failure in Daikon.

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Issue1817 {

    public static @MonotonicNonNull Map<Class<?>, List<Object>> suppressor_map;

    @RequiresNonNull("suppressor_map")
    @Pure
    public static boolean is_suppressor(Class<?> cls) {
        return suppressor_map.containsKey(cls);
    }

    public static @PolyNull Object idPoly(@PolyNull Object o) {
        return o;
    }

    @RequiresNonNull("suppressor_map")
    static int find_antecedents(
            Iterator<List<Object>> slice_iterator, Map<Class<?>, List<Object>> antecedent_map) {
        int false_cnt = 0;
        while (slice_iterator.hasNext()) {
            List<Object> slice = slice_iterator.next();
            @Nullable Object poly = null;
            for (Object inv : slice) {
                poly = idPoly(poly);
                if (!is_suppressor(inv.getClass())) {
                    continue;
                }
                List<Object> antecedents =
                        antecedent_map.computeIfAbsent(
                                inv.getClass(), __ -> new ArrayList<Object>());
                antecedents.add(inv);
            }
            if (poly != null) {
                false_cnt++;
            }
        }
        return false_cnt;
    }

    static void init_and_run(Iterator<List<Object>> it, Map<Class<?>, List<Object>> map) {
        if (suppressor_map == null) {
            suppressor_map = new HashMap<>();
        }
        find_antecedents(it, map);
    }
}
