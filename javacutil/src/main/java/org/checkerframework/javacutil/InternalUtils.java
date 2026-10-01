package org.checkerframework.javacutil;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Names;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;

/** Miscellaneous static utility methods. */
public class InternalUtils {

    // Class cannot be instantiated.
    private InternalUtils() {
        throw new AssertionError("Class InternalUtils cannot be instantiated.");
    }

    /**
     * Helper function to extract the javac Context from the javac processing environment.
     *
     * @param env the processing environment
     * @return the javac Context
     */
    public static Context getJavacContext(ProcessingEnvironment env) {
        return ((JavacProcessingEnvironment) env).getContext();
    }

    /**
     * Obtain the class loader for {@code clazz}. If that is not available, return the system class
     * loader.
     *
     * @param clazz the class whose class loader to find
     * @return the class loader used to {@code clazz}, or the system class loader, or null if both
     *     are unavailable
     */
    public static @Nullable ClassLoader getClassLoaderForClass(Class<? extends Object> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();
        return classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader;
    }

    /**
     * Compares tree1 to tree2 by the position at which a diagnostic (e.g., an error message) for
     * the tree should be printed.
     */
    public static int compareDiagnosticPosition(Tree tree1, Tree tree2) {
        DiagnosticPosition pos1 = (DiagnosticPosition) tree1;
        DiagnosticPosition pos2 = (DiagnosticPosition) tree2;

        int preferred = Integer.compare(pos1.getPreferredPosition(), pos2.getPreferredPosition());
        if (preferred != 0) {
            return preferred;
        }

        return Integer.compare(pos1.getStartPosition(), pos2.getStartPosition());
    }

    /**
     * Returns true iff {@code name} is the constructor name {@code "<init>"}.
     *
     * <p>javac interns all {@code Name}s in a per-compilation table, so two names from the same
     * compilation are equal iff they are identical. Comparing against the table's pre-interned
     * {@code init} name is a pointer comparison, whereas {@code Name.contentEquals} decodes the
     * name's UTF-8 bytes into a fresh {@code String} on every call on byte-backed name tables
     * (javac's default before JDK 23, and what Gradle's {@code -XDuseUnsharedTable} forces on all
     * JDK versions).
     *
     * @param name a name
     * @return true iff name is "&lt;init&gt;"
     */
    public static boolean isInitName(Name name) {
        if (name instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n = (com.sun.tools.javac.util.Name) name;
            return n == names(n).init;
        }
        return name.contentEquals("<init>");
    }

    /**
     * Returns true iff {@code name} is {@code "this"}. See {@link #isInitName} for why this is
     * faster than {@code Name.contentEquals}.
     *
     * @param name a name
     * @return true iff name is "this"
     */
    public static boolean isThisName(Name name) {
        if (name instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n = (com.sun.tools.javac.util.Name) name;
            return n == names(n)._this;
        }
        return name.contentEquals("this");
    }

    /**
     * Returns true iff {@code name} is {@code "super"}. See {@link #isInitName} for why this is
     * faster than {@code Name.contentEquals}.
     *
     * @param name a name
     * @return true iff name is "super"
     */
    public static boolean isSuperName(Name name) {
        if (name instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n = (com.sun.tools.javac.util.Name) name;
            return n == names(n)._super;
        }
        return name.contentEquals("super");
    }

    /**
     * Returns true iff {@code name} is {@code "value"} (e.g., the name of an annotation element).
     * See {@link #isInitName} for why this is faster than {@code Name.contentEquals}.
     *
     * @param name a name
     * @return true iff name is "value"
     */
    public static boolean isValueName(Name name) {
        if (name instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n = (com.sun.tools.javac.util.Name) name;
            return n == names(n).value;
        }
        return name.contentEquals("value");
    }

    /**
     * Returns true iff {@code name} is {@code "java.lang.Object"}. See {@link #isInitName} for why
     * this is faster than {@code Name.contentEquals}.
     *
     * @param name a name, typically a {@code TypeElement}'s qualified name
     * @return true iff name is "java.lang.Object"
     */
    public static boolean isJavaLangObjectName(Name name) {
        if (name instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n = (com.sun.tools.javac.util.Name) name;
            return n == names(n).java_lang_Object;
        }
        return name.contentEquals("java.lang.Object");
    }

    /**
     * Returns true iff {@code name} is {@code "java.lang.Enum"}. See {@link #isInitName} for why
     * this is faster than {@code Name.contentEquals}.
     *
     * @param name a name, typically a {@code TypeElement}'s qualified name
     * @return true iff name is "java.lang.Enum"
     */
    public static boolean isJavaLangEnumName(Name name) {
        if (name instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n = (com.sun.tools.javac.util.Name) name;
            return n == names(n).java_lang_Enum;
        }
        return name.contentEquals("java.lang.Enum");
    }

    /**
     * Returns the per-compilation interned-names table that {@code n} belongs to.
     *
     * @param n a javac name
     * @return the {@code Names} table that interned {@code n}
     */
    private static Names names(com.sun.tools.javac.util.Name n) {
        return n.table.names;
    }

    /**
     * A cache mapping strings to the {@code Name} that one particular {@link
     * com.sun.tools.javac.util.Name.Table} interns them as, so that {@link #sameName} can compare
     * by identity instead of decoding the name's UTF-8 bytes on every call.
     *
     * <p>The cache pins the table it was built for; {@link #sameName} discards it whenever it sees
     * a name from a different table (i.e., a new compilation in the same JVM, as in the test suite
     * or a language server). A stale cache is therefore impossible; the worst case is a rebuild.
     */
    private static final class NameCache {
        /** The name table this cache is valid for. */
        final com.sun.tools.javac.util.Name.Table table;

        /** Maps a string to the {@code Name} that {@link #table} interns it as. */
        final ConcurrentHashMap<String, com.sun.tools.javac.util.Name> map =
                new ConcurrentHashMap<>(16);

        /**
         * Creates a cache for the given table.
         *
         * @param table the name table this cache is valid for
         */
        NameCache(com.sun.tools.javac.util.Name.Table table) {
            this.table = table;
        }
    }

    /** The current {@link NameCache}, or null if no javac name has been compared yet. */
    private static volatile @Nullable NameCache nameCache;

    /**
     * Returns true iff {@code name} has the same characters as {@code expected}.
     *
     * <p>For a javac {@code Name}, this interns {@code expected} into the name's own table (cached
     * across calls) and compares by identity, instead of {@code Name.contentEquals}, which decodes
     * the name's UTF-8 bytes into a fresh {@code String} on every call on byte-backed name tables
     * (javac's default before JDK 23, and what Gradle's {@code -XDuseUnsharedTable} forces on all
     * JDK versions). Measured ~6x faster with zero allocation there, and neutral on string-backed
     * tables.
     *
     * <p>Only use this for a bounded set of {@code expected} strings (annotation element names,
     * method names a checker matches against, identifiers that occur in the source). Each distinct
     * string is interned into the compiler's name table and cached for the duration of the
     * compilation, so unbounded or one-off probe strings would grow both. For a fixed literal,
     * prefer the dedicated helpers ({@link #isInitName}, {@link #isThisName}, ...) when one exists.
     *
     * @param name a name
     * @param expected the expected string
     * @return true iff name and expected represent the same characters
     */
    public static boolean sameName(Name name, CharSequence expected) {
        if (expected instanceof Name) {
            // Avoid expected.toString(), which decodes a javac Name.
            return sameName(name, (Name) expected);
        }
        if (name instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n = (com.sun.tools.javac.util.Name) name;
            String expectedString = expected.toString();
            NameCache c = nameCache;
            @SuppressWarnings("interning:not.interned")
            boolean needInit = (c == null || c.table != n.table);
            if (needInit) {
                c = new NameCache(n.table);
                nameCache = c;
            }
            @SuppressWarnings("nullness:dereference.of.nullable") // from needsInit
            com.sun.tools.javac.util.Name target = c.map.get(expectedString);
            if (target == null) {
                target = n.table.fromString(expectedString);
                c.map.put(expectedString, target);
            }
            return n == target;
        }
        return name.contentEquals(expected);
    }

    /**
     * Returns true iff {@code name1} and {@code name2} have the same characters.
     *
     * <p>javac interns all {@code Name}s in a per-compilation table, so two javac names from the
     * same table are equal iff they are identical; {@code Name.contentEquals(Name)} would instead
     * decode <em>both</em> names' UTF-8 bytes on byte-backed name tables. Falls back to {@code
     * contentEquals} if either name is not a javac name or the tables differ.
     *
     * @param name1 a name
     * @param name2 a name
     * @return true iff name1 and name2 represent the same characters
     */
    public static boolean sameName(Name name1, Name name2) {
        if (name1 instanceof com.sun.tools.javac.util.Name
                && name2 instanceof com.sun.tools.javac.util.Name) {
            com.sun.tools.javac.util.Name n1 = (com.sun.tools.javac.util.Name) name1;
            com.sun.tools.javac.util.Name n2 = (com.sun.tools.javac.util.Name) name2;
            @SuppressWarnings("interning:not.interned")
            boolean sameTable = (n1.table == n2.table);
            if (sameTable) {
                return n1 == n2;
            }
        }
        return name1.contentEquals(name2);
    }
}
