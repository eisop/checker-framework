package org.checkerframework.javacutil;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.plumelib.util.DeepCopyable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

/**
 * The Set interface defines many methods with respect to the equals method. This implementation of
 * Set violates those specifications, but fulfills the same property using {@link
 * AnnotationUtils#areSame} rather than equals.
 *
 * <p>For example, the specification for the contains(Object o) method says: "returns true if and
 * only if this collection contains at least one element e such that (o == null ? e == null :
 * o.equals(e))." The specification for {@link AnnotationMirrorSet#contains} is "returns true if and
 * only if this collection contains at least one element e such that (o == null ? e == null :
 * AnnotationUtils.areSame(o, e))".
 *
 * <p>AnnotationMirror is an interface and not all implementing classes provide a correct equals
 * method; therefore, the existing implementations of Set cannot be used.
 *
 * <p>Implementation note: the backing store is an {@link ArrayList} with linear scan using {@link
 * AnnotationUtils#areSame} rather than a {@code TreeSet} with {@link
 * AnnotationUtils#compareAnnotationMirrors}. In practice these sets are very small (typically 1-3
 * elements, almost always fewer than 5) so a linear scan beats a sorted structure whose comparator
 * invocation and per-node allocation exceed the benefit of O(log n) lookup.
 *
 * <p>Unmodifiability is tracked by a boolean flag.
 *
 * <p>No longer implements NavigableSet; there is no point in asking for the "first" element in
 * these sets - ask for the qualifier in a specific hierarchy.
 */
public class AnnotationMirrorSet
        implements Set<@KeyFor("this") AnnotationMirror>, DeepCopyable<AnnotationMirrorSet> {

    /** Backing list; no duplicates per {@link AnnotationUtils#areSame}. */
    private ArrayList<@KeyFor("this") AnnotationMirror> shadowList = new ArrayList<>(2);

    /** True iff this set has been made unmodifiable. Guards all mutating methods. */
    private boolean unmodifiable = false;

    /** The canonical unmodifiable empty set. */
    private static final AnnotationMirrorSet emptySet = unmodifiableSet(Collections.emptySet());

    // Constructors and factory methods

    /** Default constructor. */
    public AnnotationMirrorSet() {}

    /**
     * Creates a new {@link AnnotationMirrorSet} that contains {@code value}.
     *
     * @param value the AnnotationMirror to put in the set
     */
    @SuppressWarnings("this-escape") // `add()` is safe to call
    public AnnotationMirrorSet(AnnotationMirror value) {
        this.add(value);
    }

    /**
     * Returns a new {@link AnnotationMirrorSet} that contains the given annotation mirrors.
     *
     * @param annos the AnnotationMirrors to put in the set
     */
    @SuppressWarnings("this-escape") // `addAll()` is safe to call
    public AnnotationMirrorSet(Collection<? extends AnnotationMirror> annos) {
        this.addAll(annos);
    }

    @SuppressWarnings("keyfor:argument.type.incompatible") // transferring keys from another list
    @Override
    public AnnotationMirrorSet deepCopy() {
        AnnotationMirrorSet result = new AnnotationMirrorSet();
        result.shadowList.addAll(shadowList);
        return result;
    }

    /**
     * Make this set unmodifiable. Idempotent.
     *
     * @return this set
     */
    public @This AnnotationMirrorSet makeUnmodifiable() {
        unmodifiable = true;
        return this;
    }

    /**
     * Returns a new unmodifiable {@link AnnotationMirrorSet} that contains {@code value}.
     *
     * @param value the AnnotationMirror to put in the set
     * @return a new unmodifiable {@link AnnotationMirrorSet} that contains only {@code value}
     */
    public static AnnotationMirrorSet singleton(AnnotationMirror value) {
        AnnotationMirrorSet result = new AnnotationMirrorSet();
        result.add(value);
        result.makeUnmodifiable();
        return result;
    }

    /**
     * Returns an unmodifiable AnnotationMirrorSet with the given elements.
     *
     * @param annos the annotation mirrors that will constitute the new unmodifiable set
     * @return an unmodifiable AnnotationMirrorSet with the given elements
     */
    public static AnnotationMirrorSet unmodifiableSet(
            Collection<? extends AnnotationMirror> annos) {
        AnnotationMirrorSet result = new AnnotationMirrorSet(annos);
        result.makeUnmodifiable();
        return result;
    }

    /**
     * Returns an unmodifiable empty set.
     *
     * @return an unmodifiable empty set
     */
    public static AnnotationMirrorSet emptySet() {
        return emptySet;
    }

    /**
     * Throws {@link UnsupportedOperationException} iff this set is unmodifiable. Called by every
     * mutating method before the mutation is performed.
     */
    private void checkMutable(
            @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this) {
        if (unmodifiable) {
            throw new TypeSystemError("AnnotationMirrorSet is unmodifiable");
        }
    }

    /**
     * Linear scan: returns the index of the first element {@code areSame} to {@code am}, or -1.
     *
     * @param am annotation to find
     * @return index of matching element, or -1
     */
    private int indexOfSame(
            @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this,
            AnnotationMirror am) {
        // Explicit index loop avoids Iterator allocation on the hot path.
        for (int i = 0, n = shadowList.size(); i < n; ++i) {
            if (AnnotationUtils.areSame(shadowList.get(i), am)) {
                return i;
            }
        }
        return -1;
    }

    // Set methods

    @Override
    public int size() {
        return shadowList.size();
    }

    @Override
    public boolean isEmpty() {
        return shadowList.isEmpty();
    }

    @Override
    public boolean contains(
            @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this,
            @Nullable Object o) {
        if (!(o instanceof AnnotationMirror)) {
            return false;
        }
        return indexOfSame((AnnotationMirror) o) >= 0;
    }

    @Override
    public Iterator<@KeyFor("this") AnnotationMirror> iterator() {
        Iterator<@KeyFor("this") AnnotationMirror> it = shadowList.iterator();
        return unmodifiable ? new ReadOnlyIter<@KeyFor("this") AnnotationMirror>(it) : it;
    }

    @Override
    public Object[] toArray() {
        return shadowList.toArray();
    }

    @SuppressWarnings("nullness:toarray.nullable.elements.not.newarray") // delegation
    @Override
    public <@KeyForBottom T> @Nullable T[] toArray(@PolyNull T[] a) {
        return shadowList.toArray(a);
    }

    @SuppressWarnings("keyfor:argument.type.incompatible") // delegation
    @Override
    public boolean add(
            @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this,
            AnnotationMirror annotationMirror) {
        checkMutable();
        if (indexOfSame(annotationMirror) >= 0) {
            return false;
        }
        shadowList.add(annotationMirror);
        hashCodeCache = 0; // recompute
        return true;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        if (!(o instanceof AnnotationMirror)) {
            return false;
        }
        int idx = indexOfSame((AnnotationMirror) o);
        if (idx < 0) {
            return false;
        }
        checkMutable();
        shadowList.remove(idx);
        hashCodeCache = 0; // recompute
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(
            @UnknownInitialization(AnnotationMirrorSet.class) AnnotationMirrorSet this,
            Collection<? extends AnnotationMirror> c) {
        // True iff every element was newly added.
        // Note: this differs from Set.addAll's specified semantics.
        // TODO: check whether this difference is actually useful.
        boolean result = true;
        for (AnnotationMirror a : c) {
            if (!add(a)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        checkMutable();
        ArrayList<@KeyFor("this") AnnotationMirror> newList = new ArrayList<>(shadowList.size());
        for (Object o : c) {
            if (!(o instanceof AnnotationMirror)) {
                continue;
            }
            AnnotationMirror am = (AnnotationMirror) o;
            if (indexOfSame(am) < 0) {
                continue;
            }
            // Dedupe against newList.
            boolean dup = false;
            for (int i = 0, n = newList.size(); i < n; ++i) {
                if (AnnotationUtils.areSame(newList.get(i), am)) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                @SuppressWarnings("keyfor:argument.type.incompatible") // element came from this set
                boolean unused = newList.add(am);
            }
        }
        if (newList.size() != shadowList.size()) {
            shadowList = newList;
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = true;
        for (Object a : c) {
            if (!remove(a)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public void clear() {
        checkMutable();
        shadowList.clear();
        hashCodeCache = 0; // recompute
    }

    @Override
    public String toString() {
        return shadowList.toString();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AnnotationMirrorSet)) {
            return false;
        }
        AnnotationMirrorSet s = (AnnotationMirrorSet) o;
        if (this.size() != s.size()) {
            return false;
        }
        return containsAll(s);
    }

    /** Cache the hashCode. Recomputed if zero. */
    private int hashCodeCache = 0;

    @Override
    public int hashCode() {
        if (hashCodeCache == 0) {
            int result = 0;
            for (int i = 0, n = shadowList.size(); i < n; i++) {
                // This is a set, so ordering is not considered.
                result += AnnotationUtils.hashCode(shadowList.get(i));
            }
            hashCodeCache = result;
        }
        return hashCodeCache;
    }

    /**
     * A minimal {@link Iterator} wrapper whose {@code remove()} throws.
     *
     * @param <T> element type
     */
    private static final class ReadOnlyIter<@KeyForBottom T> implements Iterator<T> {
        /** The real iterator. */
        private final Iterator<T> it;

        /**
         * Construct a readonly iterator wrapper.
         *
         * @param it the iterator to wrap
         */
        ReadOnlyIter(Iterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
