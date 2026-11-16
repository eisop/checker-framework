package org.checkerframework.framework.type.visitor;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AtmCombo;

import java.util.Arrays;
import java.util.List;

/**
 * EquivalentAtmComboScanner is an AtmComboVisitor that accepts combinations that are identical in
 * TypeMirror structure but might differ in contained AnnotationMirrors. This method will scan the
 * individual components of the visited type pairs together.
 */
public abstract class EquivalentAtmComboScanner<RETURN_TYPE, PARAM>
        extends AbstractAtmComboVisitor<RETURN_TYPE, PARAM> {

    /**
     * The number of entries that the table of {@code Visited} initially has room for; a power of
     * two. Most scans visit only one to three pairs, so the table rarely grows.
     */
    static final int INITIAL_CAPACITY = 8;

    /** The number of slots that an entry of the table of {@code Visited} occupies. */
    static final int ENTRY_SLOTS = 3;

    /**
     * A history of type pairs that have already been visited and the return type of their visit.
     */
    protected final Visited visited = new Visited();

    /** Entry point for this scanner. */
    @Override
    public RETURN_TYPE visit(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param) {
        // Avoid the cost of IdentityHashMap.clear() when the map is already empty, which is the
        // common case for top-level equality checks on simple types.
        if (!visited.isEmpty()) {
            visited.clear();
        }
        return scan(type1, type2, param);
    }

    /**
     * In an AnnotatedTypeScanner a null type is encounter than null is returned. A user may want to
     * customize the behavior of this scanner depending on whether or not one or both types is null.
     *
     * @param type1 a nullable AnnotatedTypeMirror
     * @param type2 a nullable AnnotatedTypeMirror
     * @param param the visitor param
     * @return a subclass specific return type/value
     */
    protected abstract RETURN_TYPE scanWithNull(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param);

    protected RETURN_TYPE scan(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param) {
        if (type1 == null || type2 == null) {
            return scanWithNull(type1, type2, param);
        }

        return AtmCombo.accept(type1, type2, param, this);
    }

    /**
     * Scans {@code types1} and {@code types2} in parallel with the given parameter and returns the
     * reduced result. Uses index-based access to avoid allocating iterators over the (typically
     * unmodifiable) lists.
     *
     * @param types1 types to scan
     * @param types2 types to scan paired with {@code types1}
     * @param param the visitor parameter
     * @return the reduced result of scanning all paired types, or {@code null} if both lists are
     *     empty
     */
    protected RETURN_TYPE scan(
            List<? extends AnnotatedTypeMirror> types1,
            List<? extends AnnotatedTypeMirror> types2,
            PARAM param) {
        int n = Math.min(types1.size(), types2.size());
        if (n == 0) {
            return null;
        }
        RETURN_TYPE r = scan(types1.get(0), types2.get(0), param);
        for (int i = 1; i < n; ++i) {
            r = scanAndReduce(types1.get(i), types2.get(i), param, r);
        }
        return r;
    }

    /**
     * Scans {@code types1} and {@code types2} in parallel with the given parameter and reduces the
     * result with {@code r}.
     *
     * @param types1 types to scan
     * @param types2 types to scan paired with {@code types1}
     * @param param the visitor parameter
     * @param r result to combine with the result of scanning the paired types
     * @return the combination of {@code r} with the result of scanning all paired types
     */
    protected RETURN_TYPE scanAndReduce(
            List<? extends AnnotatedTypeMirror> types1,
            List<? extends AnnotatedTypeMirror> types2,
            PARAM param,
            RETURN_TYPE r) {
        return reduce(scan(types1, types2, param), r);
    }

    protected RETURN_TYPE scanAndReduce(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param, RETURN_TYPE r) {
        return reduce(scan(type1, type2, param), r);
    }

    protected RETURN_TYPE reduce(RETURN_TYPE add, RETURN_TYPE acc) {
        if (add == null) {
            return acc;
        }
        return add;
    }

    @Override
    public RETURN_TYPE visitArray_Array(
            AnnotatedArrayType type1, AnnotatedArrayType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getComponentType(), type2.getComponentType(), param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Declared(
            AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getTypeArguments(), type2.getTypeArguments(), param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Executable(
            AnnotatedExecutableType type1, AnnotatedExecutableType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getReturnType(), type2.getReturnType(), param);
        r = scanAndReduce(type1.getReceiverType(), type2.getReceiverType(), param, r);
        r = scanAndReduce(type1.getParameterTypes(), type2.getParameterTypes(), param, r);
        r = scanAndReduce(type1.getThrownTypes(), type2.getThrownTypes(), param, r);
        r = scanAndReduce(type1.getTypeVariables(), type2.getTypeVariables(), param, r);
        return r;
    }

    @Override
    public RETURN_TYPE visitIntersection_Intersection(
            AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }
        visited.add(type1, type2, null);

        return scan(type1.getBounds(), type2.getBounds(), param);
    }

    @Override
    public @Nullable RETURN_TYPE visitNone_None(
            AnnotatedNoType type1, AnnotatedNoType type2, PARAM param) {
        return null;
    }

    @Override
    public @Nullable RETURN_TYPE visitNull_Null(
            AnnotatedNullType type1, AnnotatedNullType type2, PARAM param) {
        return null;
    }

    @Override
    public @Nullable RETURN_TYPE visitPrimitive_Primitive(
            AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return null;
    }

    @Override
    public RETURN_TYPE visitUnion_Union(
            AnnotatedUnionType type1, AnnotatedUnionType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }

        visited.add(type1, type2, null);

        return scan(type1.getAlternatives(), type2.getAlternatives(), param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Typevar(
            AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }

        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getUpperBound(), type2.getUpperBound(), param);
        r = scanAndReduce(type1.getLowerBound(), type2.getLowerBound(), param, r);
        return r;
    }

    @Override
    public RETURN_TYPE visitWildcard_Wildcard(
            AnnotatedWildcardType type1, AnnotatedWildcardType type2, PARAM param) {
        if (visited.contains(type1, type2)) {
            return visited.getResult(type1, type2);
        }

        visited.add(type1, type2, null);

        RETURN_TYPE r = scan(type1.getExtendsBound(), type2.getExtendsBound(), param);
        r = scanAndReduce(type1.getSuperBound(), type2.getSuperBound(), param, r);
        return r;
    }

    /**
     * A history of type pairs that have already been visited and the return type of their visit.
     *
     * <p>The table, {@link #slotFor}, {@link #INITIAL_CAPACITY} and {@link #ENTRY_SLOTS} are
     * package-private so that the unit test of this class can inspect the table layout.
     */
    protected class Visited {

        /** Default constructor. */
        Visited() {}

        /**
         * The backing history of type pairs: an open-addressing hash table keyed on the identity of
         * both types of a pair, with {@link #ENTRY_SLOTS} consecutive slots per entry (the first
         * type, the second type, the result). An entry whose first slot is null is empty. The home
         * entry of a pair depends on the first type only, so the pairs that share a first type form
         * one probe run and the second type is never hashed. The table is allocated on the first
         * {@link #add} and is at most two thirds full, so a probe always ends at an empty slot.
         */
        @Nullable Object @Nullable [] table;

        /** The number of pairs in {@link #table}. */
        private int size;

        /**
         * Returns true if no pairs have been recorded.
         *
         * @return true if no pairs have been recorded
         */
        public boolean isEmpty() {
            return size == 0;
        }

        /**
         * Clears the history. A table of the initial size is cleared in place; a larger one is
         * dropped, so that the cost of clearing stays bounded.
         */
        public void clear() {
            if (table != null && table.length == INITIAL_CAPACITY * ENTRY_SLOTS) {
                Arrays.fill(table, null);
            } else {
                table = null;
            }
            size = 0;
        }

        /**
         * Returns the index of the entry for the given pair in {@code table}, or of the empty entry
         * at which the pair would be inserted.
         *
         * @param table the table to probe
         * @param type1 the first type
         * @param type2 the second type
         * @return the slot index of the entry for the pair, or of an empty entry
         */
        @SuppressWarnings("interning:not.interned") // the table is keyed on identity
        int slotFor(@Nullable Object[] table, Object type1, Object type2) {
            int h = System.identityHashCode(type1);
            h ^= (h >>> 16);
            int mask = table.length / ENTRY_SLOTS - 1;
            int i = (h & mask) * ENTRY_SLOTS;
            while (true) {
                Object k1 = table[i];
                if (k1 == null || (k1 == type1 && table[i + 1] == type2)) {
                    return i;
                }
                i += ENTRY_SLOTS;
                if (i == table.length) {
                    i = 0;
                }
            }
        }

        /**
         * Returns true if the given pair has been recorded.
         *
         * @param type1 the first type
         * @param type2 the second type
         * @return true if the given pair has been recorded
         */
        public boolean contains(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
            @Nullable Object[] table = this.table;
            return table != null && table[slotFor(table, type1, type2)] != null;
        }

        /**
         * Returns the result recorded for the given pair, or null if none was recorded.
         *
         * @param type1 the first type
         * @param type2 the second type
         * @return the result recorded for the given pair, or null if none was recorded
         */
        @SuppressWarnings({
            "unchecked", // the result slot of an entry holds a RETURN_TYPE
            // The cast type has the qualifiers of the operand, which RETURN_TYPE need not accept.
            "compilermessages:return.type.incompatible",
            "formatter:return.type.incompatible",
            "interning:return.type.incompatible",
            "optionalimpl:return.type.incompatible",
            "rlccalledmethods:return.type.incompatible",
            "signature:return.type.incompatible"
        })
        public @Nullable RETURN_TYPE getResult(
                AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
            @Nullable Object[] table = this.table;
            if (table == null) {
                return null;
            }
            int i = slotFor(table, type1, type2);
            if (table[i] == null) {
                return null;
            }
            return (RETURN_TYPE) table[i + 2];
        }

        /**
         * Add a new pair to the history.
         *
         * @param type1 the first type
         * @param type2 the second type
         * @param ret the result
         */
        public void add(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, RETURN_TYPE ret) {
            @Nullable Object[] table = this.table;
            if (table == null) {
                table = new @Nullable Object[INITIAL_CAPACITY * ENTRY_SLOTS];
                this.table = table;
            }
            int i = slotFor(table, type1, type2);
            if (table[i] == null) {
                int capacity = table.length / ENTRY_SLOTS;
                if (3 * (size + 1) > 2 * capacity) {
                    table = grow(table);
                    this.table = table;
                    i = slotFor(table, type1, type2);
                }
                table[i] = type1;
                table[i + 1] = type2;
                size++;
            }
            table[i + 2] = ret;
        }

        /**
         * Returns a table with twice the capacity of {@code old} that holds the same entries.
         *
         * @param old the table to grow
         * @return a table with twice the capacity of {@code old} that holds the same entries
         */
        private @Nullable Object[] grow(@Nullable Object[] old) {
            @Nullable Object[] table = new @Nullable Object[old.length * 2];
            for (int j = 0; j < old.length; j += ENTRY_SLOTS) {
                Object k1 = old[j];
                if (k1 != null) {
                    Object k2 = old[j + 1];
                    assert k2 != null : "@AssumeAssertion(nullness): both keys of an entry are set";
                    int i = slotFor(table, k1, k2);
                    table[i] = k1;
                    table[i + 1] = k2;
                    table[i + 2] = old[j + 2];
                }
            }
            return table;
        }
    }
}
