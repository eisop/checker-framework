package org.checkerframework.framework.flow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A Map that defers copying its internal Map until it is mutated.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class CopyOnWriteMap<K, V> implements Map<K, V> {
    /** The underlying map that stores the entries. */
    private Map<K, V> delegate;

    /** True if the delegate map is currently shared with another CopyOnWriteMap instance. */
    private boolean shared;

    /**
     * Creates a new CopyOnWriteMap.
     *
     * @param initial the initial map to wrap
     * @param shared whether the initial map is already shared with another instance
     */
    public CopyOnWriteMap(Map<K, V> initial, boolean shared) {
        this.delegate = initial;
        this.shared = shared;
    }

    /** Ensures the delegate is not shared before mutation. */
    private void ensureUnshared() {
        if (shared) {
            delegate = new HashMap<>(delegate);
            shared = false;
        }
    }

    /**
     * Creates a copy of this map by sharing the underlying delegate.
     *
     * @return a copy of this map
     */
    public CopyOnWriteMap<K, V> copy() {
        this.shared = true;
        return new CopyOnWriteMap<>(this.delegate, true);
    }

    /**
     * Replaces this map's state with the state of the other map, sharing its delegate.
     *
     * @param other the map to copy from
     */
    public void copyFrom(CopyOnWriteMap<K, V> other) {
        this.delegate = other.delegate;
        this.shared = true;
        other.shared = true;
        invalidateHash();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return delegate.get(key);
    }

    /** The cached hash code. */
    private int hashCodeCache = 0;

    /** Invalidates the cached hash code. */
    private void invalidateHash() {
        hashCodeCache = 0;
    }

    @Override
    public V put(K key, V value) {
        ensureUnshared();
        invalidateHash();
        return delegate.put(key, value);
    }

    @Override
    public V remove(Object key) {
        ensureUnshared();
        invalidateHash();
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (m.isEmpty()) {
            return;
        }
        ensureUnshared();
        invalidateHash();
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        if (delegate.isEmpty()) {
            return;
        }
        if (shared) {
            // Optimization: if shared, just drop the reference and create a new empty map.
            // This avoids making a full copy of the shared data just to clear it.
            delegate = new HashMap<>();
            shared = false;
        } else {
            delegate.clear();
        }
        invalidateHash();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        // The default Map.putIfAbsent only writes when the key is absent, but it can write
        // through the (potentially shared) delegate, so ensure we own the delegate first.
        ensureUnshared();
        invalidateHash();
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        ensureUnshared();
        invalidateHash();
        return delegate.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        ensureUnshared();
        invalidateHash();
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        ensureUnshared();
        invalidateHash();
        return delegate.replace(key, value);
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        // The default Map.replaceAll mutates entries in place via Entry.setValue on the
        // delegate's entrySet. If the delegate is shared, that would corrupt every other
        // CopyOnWriteMap sharing it, so copy first.
        ensureUnshared();
        invalidateHash();
        delegate.replaceAll(function);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        ensureUnshared();
        invalidateHash();
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(
            K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        ensureUnshared();
        invalidateHash();
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        ensureUnshared();
        invalidateHash();
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public V merge(
            K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        ensureUnshared();
        invalidateHash();
        return delegate.merge(key, value, remappingFunction);
    }

    @Override
    public Set<K> keySet() {
        // Return an unmodifiable view: mutating through the view would bypass copy-on-write
        // and could corrupt a shared delegate.
        return Collections.unmodifiableSet(delegate.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(delegate.values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // Use unmodifiableMap(...).entrySet() rather than unmodifiableSet(entrySet()): the
        // former also wraps each Entry so that Entry.setValue is rejected. A plain
        // unmodifiableSet would still allow setValue, which writes through to (and could
        // corrupt) a shared delegate.
        return Collections.unmodifiableMap(delegate).entrySet();
    }

    @Override
    @SuppressWarnings("interning:not.interned")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CopyOnWriteMap) {
            CopyOnWriteMap<?, ?> other = (CopyOnWriteMap<?, ?>) o;
            // Fast path: if both maps are sharing the exact same underlying delegate,
            // they are inherently equal. This skips a potentially expensive deep comparison.
            if (this.delegate == other.delegate) {
                return true;
            }
            return delegate.equals(other.delegate);
        }
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        if (hashCodeCache == 0) {
            int h = delegate.hashCode();
            hashCodeCache = h == 0 ? 1 : h;
        }
        return hashCodeCache;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
