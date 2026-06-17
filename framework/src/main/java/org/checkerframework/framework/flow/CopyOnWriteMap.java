package org.checkerframework.framework.flow;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
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
