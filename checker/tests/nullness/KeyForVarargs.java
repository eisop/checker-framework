// Test that a dependent type annotation on a varargs parameter is viewpoint-adapted at a call that
// passes the varargs elements individually.

import org.checkerframework.checker.nullness.qual.KeyFor;

import java.util.Map;

public class KeyForVarargs {
    static void one(Map<String, Integer> m, @KeyFor("#1") String key) {}

    static void many(Map<String, Integer> m, @KeyFor("#1") String... keys) {}

    @SafeVarargs
    static <T> void generic(Map<T, Integer> m, @KeyFor("#1") T... keys) {}

    static void arrays(Map<String, Integer> m, @KeyFor("#1") String[]... keys) {}

    static class Ctor {
        Ctor(Map<String, Integer> m, @KeyFor("#1") String... keys) {}
    }

    static void caller(
            Map<String, Integer> map,
            Map<String, Integer> other,
            @KeyFor("#1") String k,
            @KeyFor("#1") String k2,
            @KeyFor("#2") String o,
            @KeyFor("#1") String[] ka) {
        one(map, k);
        many(map, new @KeyFor("map") String[] {k});
        many(map);
        many(map, k);
        many(map, k, k2);
        generic(map, k);
        generic(map, k, k2);
        arrays(map, ka);
        arrays(map, ka, ka);
        new Ctor(map, k, k2);

        // :: error: (argument.type.incompatible)
        many(map, o);
        // :: error: (argument.type.incompatible)
        many(map, k, o);
        // :: error: (argument.type.incompatible)
        many(other, k);
        // :: error: (argument.type.incompatible)
        generic(other, k);
        // :: error: (argument.type.incompatible)
        new Ctor(map, k, o);
    }
}
