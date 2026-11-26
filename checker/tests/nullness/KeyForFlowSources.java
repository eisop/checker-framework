// Each method obtains a key from a different source and looks it up in the map. A lookup is
// @NonNull only if dataflow refines the type of the local variable that holds the key.

import org.checkerframework.checker.nullness.qual.EnsuresKeyFor;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class KeyForFlowSources {

    static final Map<String, String> m = new HashMap<>();

    static final @KeyFor("KeyForFlowSources.m") String KEY = makeKey("a");

    static @KeyFor("KeyForFlowSources.m") String makeKey(String k) {
        m.put(k, "b");
        return k;
    }

    @EnsuresKeyFor(value = "#1", map = "KeyForFlowSources.m")
    static void ensureKey(String k) {
        m.put(k, "x");
    }

    static class Box<T> {
        T val;

        Box(T v) {
            val = v;
        }

        T get() {
            return val;
        }
    }

    // :: error: (type.argument.type.incompatible)
    static class KeyBox extends Box<@KeyFor("KeyForFlowSources.m") String> {
        KeyBox(@KeyFor("KeyForFlowSources.m") String v) {
            super(v);
        }
    }

    static class IndirectKeyBox extends KeyBox {
        IndirectKeyBox(@KeyFor("KeyForFlowSources.m") String v) {
            super(v);
        }
    }

    interface KeyConsumer {
        void accept(@KeyFor("KeyForFlowSources.m") String s);
    }

    interface KeySupplier {
        @KeyFor("KeyForFlowSources.m") String make();
    }

    // No key is involved.
    int noKey(int x, String s) {
        String t = s + x;
        return t.length();
    }

    // No key is involved.
    Supplier<String> noKeyLambdaAndLocalClass(String s) {
        Supplier<String> r = () -> s + "x";
        class Local {
            String f(String a) {
                String b = a;
                return b + s;
            }
        }
        return () -> new Local().f(r.get());
    }

    @NonNull String viaParameter(@KeyFor("KeyForFlowSources.m") String p) {
        String k = p;
        return m.get(k);
    }

    @NonNull String viaSupertype(KeyBox o) {
        String k = o.get();
        return m.get(k);
    }

    @NonNull String viaIndirectSupertype(IndirectKeyBox o) {
        String k = o.get();
        return m.get(k);
    }

    @NonNull String viaInheritedField(KeyBox o) {
        String k = o.val;
        return m.get(k);
    }

    void viaLambdaParameter() {
        KeyConsumer f =
                s -> {
                    String t = s;
                    @NonNull String v = m.get(t);
                };
    }

    @NonNull String viaFunctionalInterface(KeySupplier sup) {
        String k = sup.make();
        return m.get(k);
    }

    @NonNull String viaMethodReference() {
        Supplier<@KeyFor("KeyForFlowSources.m") String> sup = KeyForFlowSources::keyConstant;
        String k = sup.get();
        return m.get(k);
    }

    static @KeyFor("KeyForFlowSources.m") String keyConstant() {
        return KEY;
    }

    @NonNull String viaContainsKey(String k) {
        if (m.containsKey(k)) {
            return m.get(k);
        }
        return "";
    }

    @NonNull String viaPut(String k) {
        m.put(k, "v");
        return m.get(k);
    }

    @NonNull String viaContract(String k) {
        ensureKey(k);
        return m.get(k);
    }

    @NonNull String viaStaticField() {
        String k = KEY;
        return m.get(k);
    }

    @NonNull String viaKeySet() {
        for (String k : m.keySet()) {
            String j = k;
            return m.get(j);
        }
        return "";
    }

    void viaCapturedParameter(@KeyFor("KeyForFlowSources.m") String p) {
        String k = p;
        Runnable r =
                new Runnable() {
                    @Override
                    public void run() {
                        String j = k;
                        @NonNull String v = m.get(j);
                    }
                };
    }

    void viaCapturedRefinedLocal(String p) {
        m.put(p, "x");
        String k = p;
        Runnable r =
                new Runnable() {
                    @Override
                    public void run() {
                        String j = k;
                        @NonNull String v = m.get(j);
                    }
                };
        Runnable l =
                () -> {
                    String j = k;
                    @NonNull String v = m.get(j);
                };
    }

    <T extends @KeyFor("KeyForFlowSources.m") String> @NonNull String viaTypeVariable(T t) {
        String j = t;
        return m.get(j);
    }

    @NonNull String viaTernary(KeyBox o, boolean c) {
        String j = c ? o.get() : new IndirectKeyBox(KEY).get();
        return m.get(j);
    }

    @NonNull String viaLocalClass() {
        class LocalSupplier {
            @KeyFor("KeyForFlowSources.m") String make() {
                return KEY;
            }
        }
        String k = new LocalSupplier().make();
        return m.get(k);
    }

    static class InstanceMap {
        final Map<String, String> fm = new HashMap<>();
        final @KeyFor("fm") String key;

        InstanceMap(String a) {
            fm.put(a, "b");
            key = a;
        }

        @NonNull String viaFinalField() {
            String k = key;
            return fm.get(k);
        }
    }

    static class InferredFinalField {
        final String key = makeKey("c");

        @NonNull String viaInitializer() {
            String k = key;
            return m.get(k);
        }
    }
}
