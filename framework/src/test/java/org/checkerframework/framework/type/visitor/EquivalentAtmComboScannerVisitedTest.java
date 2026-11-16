package org.checkerframework.framework.type.visitor;

import com.sun.source.util.JavacTask;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.testchecker.h1h2checker.H1H2Checker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;

import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Unit tests for {@link EquivalentAtmComboScanner.Visited}, the recursion guard of {@code
 * EqualityAtmComparer} (and so of {@link AnnotatedTypeMirror#equals}) and of the poly collector.
 *
 * <p>{@code Visited} is an open-addressing table keyed on the identity of both types of a pair. The
 * checker test suites exercise it only through type comparisons, which rarely record more than a
 * few pairs, so these tests target its boundaries directly: identity (not equality) of the keys,
 * colliding pairs and probes that wrap around the end of the table, growth across every threshold
 * with every pair and result preserved, replacing a pair's result (including a null result, which
 * {@code contains} distinguishes from an absent pair), and clearing a table of the initial size (in
 * place) and a grown one (dropped). The last test compares the table against the nested {@link
 * IdentityHashMap}s it replaced over a seeded sequence of operations.
 *
 * <p>Collisions are made deterministic without assuming anything about identity hash codes: a test
 * creates fresh first types until {@link EquivalentAtmComboScanner.Visited#slotFor} maps a pair to
 * the wanted home entry of an empty table of the relevant capacity, and then inspects the table to
 * confirm where the entry landed. The home entry depends on the first type only, so pairs that
 * share a first type collide by construction, which one test covers directly.
 */
public class EquivalentAtmComboScannerVisitedTest {

    /** The number of entries a table has room for before its first growth. */
    private static final int INITIAL_CAPACITY = EquivalentAtmComboScanner.INITIAL_CAPACITY;

    /** The number of slots per entry. */
    private static final int ENTRY_SLOTS = EquivalentAtmComboScanner.ENTRY_SLOTS;

    /** A scanner that exposes its {@code Visited} history. */
    private static class TestScanner extends EquivalentAtmComboScanner<String, Void> {

        /** Creates a scanner. */
        TestScanner() {}

        @Override
        protected @Nullable String scanWithNull(
                AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, Void param) {
            return null;
        }

        /**
         * Returns a new, empty history.
         *
         * @return a new, empty history
         */
        Visited newVisited() {
            return new Visited();
        }

        /**
         * Returns the history that {@link #visit} uses.
         *
         * @return the history that {@link #visit} uses
         */
        Visited history() {
            return visited;
        }
    }

    /** The scanner whose {@code Visited} instances are tested. */
    private static final TestScanner SCANNER = new TestScanner();

    /** The factory that creates the test types. */
    private static @Nullable AnnotatedTypeFactory factory;

    /** The underlying type of every test type. */
    private static @Nullable TypeMirror stringType;

    /**
     * Returns an in-memory Java source file.
     *
     * @param className the simple name of the class, used only to form the file URI
     * @param code the source text of the file
     * @return a {@link JavaFileObject} that yields {@code code} as its content
     */
    private static JavaFileObject source(String className, String code) {
        return new SimpleJavaFileObject(
                URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
    }

    /**
     * Runs a checker on a trivial class so that its type factory can create annotated types. {@link
     * AnnotatedTypeMirror#createType} needs a factory; which checker provides it is irrelevant to
     * these tests, which use the types only as identity keys.
     *
     * @throws Exception if the compiler task cannot be created
     */
    @BeforeClass
    public static void createFactory() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        H1H2Checker checker = new H1H2Checker();
        JavaCompiler.CompilationTask compilationTask =
                compiler.getTask(
                        new StringWriter(),
                        null,
                        diagnostics,
                        Arrays.asList(
                                "-classpath",
                                System.getProperty("java.class.path"),
                                "-AnoJreVersionCheck"),
                        null,
                        Collections.singletonList(
                                source("VisitedTestInput", "class VisitedTestInput {}\n")));
        JavacTask javacTask = (JavacTask) compilationTask;
        javacTask.setProcessors(Collections.singletonList(checker));
        javacTask.analyze();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            Assert.assertNotEquals(
                    "unexpected error: " + d.getMessage(null), Diagnostic.Kind.ERROR, d.getKind());
        }
        factory = checker.getTypeFactory();
        stringType = javacTask.getElements().getTypeElement("java.lang.String").asType();
    }

    /**
     * Returns a new annotated type for {@code java.lang.String}. Every call returns a distinct
     * object, which {@link AnnotatedTypeMirror#equals} considers equal to every other one.
     *
     * @return a new annotated {@code String} type
     */
    private static AnnotatedTypeMirror newType() {
        AnnotatedTypeFactory f = factory;
        TypeMirror t = stringType;
        Assert.assertNotNull(f);
        Assert.assertNotNull(t);
        return AnnotatedTypeMirror.createType(t, f, false);
    }

    /**
     * Returns the entry of a table with room for {@code capacity} entries at which the pair would
     * be stored if no other pair were in the way.
     *
     * @param visited the history whose slot computation to use
     * @param type1 the first type
     * @param type2 the second type
     * @param capacity the number of entries the table has room for
     * @return the home entry of the pair
     */
    private static int homeEntry(
            EquivalentAtmComboScanner<String, Void>.Visited visited,
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2,
            int capacity) {
        @Nullable Object[] empty = new @Nullable Object[capacity * ENTRY_SLOTS];
        return visited.slotFor(empty, type1, type2) / ENTRY_SLOTS;
    }

    /**
     * Returns a new type {@code t} such that every pair ({@code t}, x) has the given home entry in
     * a table with room for {@code capacity} entries.
     *
     * @param visited the history whose slot computation to use
     * @param capacity the number of entries the table has room for
     * @param home the wanted home entry
     * @return the first type of pairs with home entry {@code home}
     */
    private static AnnotatedTypeMirror firstWithHome(
            EquivalentAtmComboScanner<String, Void>.Visited visited, int capacity, int home) {
        // The home entry is a function of one identity hash code, so a handful of fresh types
        // suffices for each of the `capacity` possible homes; the bound only guards the loop.
        AnnotatedTypeMirror any = newType();
        for (int i = 0; i < 100_000; i++) {
            AnnotatedTypeMirror first = newType();
            if (homeEntry(visited, first, any, capacity) == home) {
                Assert.assertEquals(
                        "the home entry does not depend on the second type",
                        home,
                        homeEntry(visited, first, newType(), capacity));
                return first;
            }
        }
        throw new AssertionError("no pair with home entry " + home + " found");
    }

    /**
     * Returns the table of the history.
     *
     * @param visited a history
     * @return its table
     */
    private static @Nullable Object[] table(
            EquivalentAtmComboScanner<String, Void>.Visited visited) {
        @Nullable Object[] table = visited.table;
        Assert.assertNotNull("the table is allocated on the first add", table);
        return table;
    }

    /**
     * Returns the number of entries the table of the history has room for.
     *
     * @param visited a history
     * @return the number of entries its table has room for
     */
    private static int capacity(EquivalentAtmComboScanner<String, Void>.Visited visited) {
        return table(visited).length / ENTRY_SLOTS;
    }

    /**
     * Returns the first type stored at the given entry of the table, or null if the entry is empty.
     *
     * @param visited a history
     * @param entry an entry index
     * @return the first type stored at the entry, or null if it is empty
     */
    private static @Nullable Object firstTypeAt(
            EquivalentAtmComboScanner<String, Void>.Visited visited, int entry) {
        return table(visited)[entry * ENTRY_SLOTS];
    }

    /**
     * Returns the number of occupied entries of the table of the history.
     *
     * @param visited a history
     * @return the number of occupied entries
     */
    private static int occupied(EquivalentAtmComboScanner<String, Void>.Visited visited) {
        @Nullable Object[] table = table(visited);
        int n = 0;
        for (int i = 0; i < table.length; i += ENTRY_SLOTS) {
            if (table[i] != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * Asserts that the pair is recorded with the given result.
     *
     * @param visited a history
     * @param type1 the first type
     * @param type2 the second type
     * @param result the expected result
     */
    private static void assertRecorded(
            EquivalentAtmComboScanner<String, Void>.Visited visited,
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2,
            @Nullable String result) {
        Assert.assertTrue(visited.contains(type1, type2));
        Assert.assertEquals(result, visited.getResult(type1, type2));
    }

    /**
     * Asserts that the pair is not recorded.
     *
     * @param visited a history
     * @param type1 the first type
     * @param type2 the second type
     */
    private static void assertAbsent(
            EquivalentAtmComboScanner<String, Void>.Visited visited,
            AnnotatedTypeMirror type1,
            AnnotatedTypeMirror type2) {
        Assert.assertFalse(visited.contains(type1, type2));
        Assert.assertNull(visited.getResult(type1, type2));
    }

    /** Pairs are keyed on the identity of both types, in order, not on their equality. */
    @Test
    public void keysAreIdentityPairs() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        AnnotatedTypeMirror a = newType();
        AnnotatedTypeMirror b = newType();
        AnnotatedTypeMirror c = newType();
        Assert.assertNotSame(a, b);
        Assert.assertEquals("the test types are structurally equal", a, b);

        Assert.assertTrue(visited.isEmpty());
        assertAbsent(visited, a, b);

        visited.add(a, b, "ab");
        Assert.assertFalse(visited.isEmpty());
        assertRecorded(visited, a, b, "ab");
        // The reversed pair, the pairs that share one component, and a pair of a structurally
        // equal but distinct type are different keys.
        assertAbsent(visited, b, a);
        assertAbsent(visited, a, a);
        assertAbsent(visited, b, b);
        assertAbsent(visited, a, c);
        assertAbsent(visited, c, b);

        visited.add(b, a, "ba");
        visited.add(a, a, "aa");
        visited.add(a, c, "ac");
        visited.add(c, b, "cb");
        assertRecorded(visited, a, b, "ab");
        assertRecorded(visited, b, a, "ba");
        assertRecorded(visited, a, a, "aa");
        assertRecorded(visited, a, c, "ac");
        assertRecorded(visited, c, b, "cb");
        assertAbsent(visited, b, b);
        assertAbsent(visited, c, a);
        assertAbsent(visited, c, c);
        Assert.assertEquals(5, occupied(visited));
    }

    /** Colliding pairs probe forward to the next empty entries and are found there. */
    @Test
    public void collidingPairsProbeForward() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        int home = 3;
        AnnotatedTypeMirror second = newType();
        AnnotatedTypeMirror f1 = firstWithHome(visited, INITIAL_CAPACITY, home);
        AnnotatedTypeMirror f2 = firstWithHome(visited, INITIAL_CAPACITY, home);
        AnnotatedTypeMirror f3 = firstWithHome(visited, INITIAL_CAPACITY, home);
        AnnotatedTypeMirror absent = firstWithHome(visited, INITIAL_CAPACITY, home);
        AnnotatedTypeMirror absentNext = firstWithHome(visited, INITIAL_CAPACITY, home + 1);

        visited.add(f1, second, "f1");
        visited.add(f2, second, "f2");
        visited.add(f3, second, null);
        Assert.assertEquals(INITIAL_CAPACITY, capacity(visited));
        Assert.assertSame(f1, firstTypeAt(visited, home));
        Assert.assertSame(f2, firstTypeAt(visited, home + 1));
        Assert.assertSame(f3, firstTypeAt(visited, home + 2));
        Assert.assertNull(firstTypeAt(visited, home + 3));
        Assert.assertEquals(3, occupied(visited));

        assertRecorded(visited, f1, second, "f1");
        assertRecorded(visited, f2, second, "f2");
        assertRecorded(visited, f3, second, null);
        // A probe for an absent pair passes the three occupied entries and stops at the empty one.
        assertAbsent(visited, absent, second);
        assertAbsent(visited, absentNext, second);
        assertAbsent(visited, second, f1);
        assertAbsent(visited, second, f2);

        // Replacing a result does not move or duplicate the entry.
        visited.add(f2, second, "f2'");
        Assert.assertEquals(3, occupied(visited));
        Assert.assertSame(f2, firstTypeAt(visited, home + 1));
        assertRecorded(visited, f2, second, "f2'");
    }

    /** Pairs that share a first type have the same home entry and form one probe run. */
    @Test
    public void pairsSharingTheFirstTypeShareAProbeRun() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        AnnotatedTypeMirror first = firstWithHome(visited, INITIAL_CAPACITY, 2);
        AnnotatedTypeMirror s1 = newType();
        AnnotatedTypeMirror s2 = newType();
        AnnotatedTypeMirror s3 = newType();
        AnnotatedTypeMirror absent = newType();

        visited.add(first, s1, "s1");
        visited.add(first, s2, null);
        visited.add(first, s3, "s3");
        Assert.assertSame(first, firstTypeAt(visited, 2));
        Assert.assertSame(first, firstTypeAt(visited, 3));
        Assert.assertSame(first, firstTypeAt(visited, 4));
        Assert.assertSame(s1, table(visited)[2 * ENTRY_SLOTS + 1]);
        Assert.assertSame(s2, table(visited)[3 * ENTRY_SLOTS + 1]);
        Assert.assertSame(s3, table(visited)[4 * ENTRY_SLOTS + 1]);
        Assert.assertEquals(3, occupied(visited));

        assertRecorded(visited, first, s1, "s1");
        assertRecorded(visited, first, s2, null);
        assertRecorded(visited, first, s3, "s3");
        // The probe for an absent pair with the same first type passes the whole run.
        assertAbsent(visited, first, absent);
        assertAbsent(visited, first, first);
        assertAbsent(visited, s1, first);

        visited.add(first, s2, "s2");
        Assert.assertEquals(3, occupied(visited));
        Assert.assertSame(first, firstTypeAt(visited, 3));
        assertRecorded(visited, first, s2, "s2");
    }

    /** A probe that reaches the end of the table continues at its first entry. */
    @Test
    public void probeWrapsAroundTheEndOfTheTable() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        int last = INITIAL_CAPACITY - 1;
        AnnotatedTypeMirror second = newType();
        AnnotatedTypeMirror atLast = firstWithHome(visited, INITIAL_CAPACITY, last);
        AnnotatedTypeMirror atZero = firstWithHome(visited, INITIAL_CAPACITY, 0);
        AnnotatedTypeMirror wrapsToOne = firstWithHome(visited, INITIAL_CAPACITY, last);
        AnnotatedTypeMirror wrapsToTwo = firstWithHome(visited, INITIAL_CAPACITY, last);
        AnnotatedTypeMirror absentLast = firstWithHome(visited, INITIAL_CAPACITY, last);
        AnnotatedTypeMirror absentZero = firstWithHome(visited, INITIAL_CAPACITY, 0);

        visited.add(atLast, second, "last");
        visited.add(atZero, second, "zero");
        // Both home entries are taken, so this pair wraps around to entry 1 ...
        visited.add(wrapsToOne, second, null);
        // ... and this one to entry 2.
        visited.add(wrapsToTwo, second, "two");
        Assert.assertEquals(INITIAL_CAPACITY, capacity(visited));
        Assert.assertSame(atLast, firstTypeAt(visited, last));
        Assert.assertSame(atZero, firstTypeAt(visited, 0));
        Assert.assertSame(wrapsToOne, firstTypeAt(visited, 1));
        Assert.assertSame(wrapsToTwo, firstTypeAt(visited, 2));
        Assert.assertSame(second, table(visited)[last * ENTRY_SLOTS + 1]);
        Assert.assertSame(second, table(visited)[1]);
        Assert.assertSame(second, table(visited)[ENTRY_SLOTS + 1]);
        Assert.assertSame(second, table(visited)[2 * ENTRY_SLOTS + 1]);
        Assert.assertEquals(4, occupied(visited));

        assertRecorded(visited, atLast, second, "last");
        assertRecorded(visited, atZero, second, "zero");
        assertRecorded(visited, wrapsToOne, second, null);
        assertRecorded(visited, wrapsToTwo, second, "two");
        // Probes for absent pairs wrap around too, and stop at entry 3.
        assertAbsent(visited, absentLast, second);
        assertAbsent(visited, absentZero, second);
        assertAbsent(visited, second, atLast);
        assertAbsent(visited, second, wrapsToOne);

        visited.add(wrapsToOne, second, "one");
        Assert.assertEquals(4, occupied(visited));
        assertRecorded(visited, wrapsToOne, second, "one");
    }

    /**
     * The table doubles exactly when the next pair would make it more than two thirds full, and
     * every pair and result survives each growth.
     */
    @Test
    public void growthPreservesEveryPairAndResult() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        List<AnnotatedTypeMirror> firsts = new ArrayList<>();
        List<AnnotatedTypeMirror> seconds = new ArrayList<>();
        List<@Nullable String> results = new ArrayList<>();
        List<Integer> growthPoints = new ArrayList<>();
        int previousCapacity = 0;
        int n = 100;
        for (int i = 1; i <= n; i++) {
            AnnotatedTypeMirror first = newType();
            AnnotatedTypeMirror second = newType();
            @Nullable String result = i % 7 == 0 ? null : "r" + i;
            visited.add(first, second, result);
            firsts.add(first);
            seconds.add(second);
            results.add(result);

            int capacity = capacity(visited);
            if (capacity != previousCapacity) {
                Assert.assertEquals(
                        "the table doubles",
                        previousCapacity == 0 ? INITIAL_CAPACITY : 2 * previousCapacity,
                        capacity);
                growthPoints.add(i);
                previousCapacity = capacity;
            }
            Assert.assertTrue("at most two thirds full", 3 * i <= 2 * capacity);
            Assert.assertTrue(
                    "grown only when needed", capacity == INITIAL_CAPACITY || 3 * i > capacity);
            Assert.assertEquals(i, occupied(visited));
            for (int j = 0; j < i; j++) {
                assertRecorded(visited, firsts.get(j), seconds.get(j), results.get(j));
                assertAbsent(visited, seconds.get(j), firsts.get(j));
            }
        }
        // The initial table is allocated by the first add; with room for 8 entries it is grown by
        // the 6th add (6 > 2/3 * 8), the next by the 11th (11 > 2/3 * 16), and so on.
        Assert.assertEquals(Arrays.asList(1, 6, 11, 22, 43, 86), growthPoints);
        Assert.assertEquals(32 * INITIAL_CAPACITY, capacity(visited));
    }

    /**
     * Adding a pair again replaces its result, and a null result is distinguished from an absent
     * pair by {@code contains}.
     */
    @Test
    public void addReplacesTheResult() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        AnnotatedTypeMirror a = newType();
        AnnotatedTypeMirror b = newType();

        visited.add(a, b, null);
        Assert.assertTrue(visited.contains(a, b));
        Assert.assertNull(visited.getResult(a, b));
        assertAbsent(visited, b, a);
        Assert.assertEquals(1, occupied(visited));

        visited.add(a, b, "x");
        assertRecorded(visited, a, b, "x");
        visited.add(a, b, "y");
        assertRecorded(visited, a, b, "y");
        visited.add(a, b, null);
        Assert.assertTrue(visited.contains(a, b));
        Assert.assertNull(visited.getResult(a, b));
        Assert.assertEquals(1, occupied(visited));
        Assert.assertEquals(INITIAL_CAPACITY, capacity(visited));

        // Replacing the result of a pair many times never counts as a new pair, so the table does
        // not grow.
        for (int i = 0; i < 50; i++) {
            visited.add(a, b, "r" + i);
        }
        assertRecorded(visited, a, b, "r49");
        Assert.assertEquals(1, occupied(visited));
        Assert.assertEquals(INITIAL_CAPACITY, capacity(visited));
    }

    /** Clearing a table of the initial size nulls it in place, and the table is reused. */
    @Test
    public void clearReusesTheInitialTable() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        Assert.assertNull(visited.table);
        visited.clear();
        Assert.assertNull("clearing an unallocated history allocates nothing", visited.table);
        Assert.assertTrue(visited.isEmpty());

        List<AnnotatedTypeMirror> firsts = new ArrayList<>();
        List<AnnotatedTypeMirror> seconds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            firsts.add(newType());
            seconds.add(newType());
            visited.add(firsts.get(i), seconds.get(i), "r" + i);
        }
        @Nullable Object[] before = table(visited);
        Assert.assertEquals(INITIAL_CAPACITY, capacity(visited));

        visited.clear();
        Assert.assertSame("a table of the initial size is kept", before, visited.table);
        for (Object slot : before) {
            Assert.assertNull(slot);
        }
        Assert.assertTrue(visited.isEmpty());
        for (int i = 0; i < 5; i++) {
            assertAbsent(visited, firsts.get(i), seconds.get(i));
        }

        // The cleared table is reused and starts counting from zero: five more pairs fit in it
        // without growth.
        for (int i = 0; i < 5; i++) {
            visited.add(seconds.get(i), firsts.get(i), "s" + i);
        }
        Assert.assertSame(before, visited.table);
        Assert.assertEquals(5, occupied(visited));
        for (int i = 0; i < 5; i++) {
            assertRecorded(visited, seconds.get(i), firsts.get(i), "s" + i);
            assertAbsent(visited, firsts.get(i), seconds.get(i));
        }
    }

    /** Clearing a grown table drops it, and the next add allocates a table of the initial size. */
    @Test
    public void clearDropsAGrownTable() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        List<AnnotatedTypeMirror> firsts = new ArrayList<>();
        List<AnnotatedTypeMirror> seconds = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            firsts.add(newType());
            seconds.add(newType());
            visited.add(firsts.get(i), seconds.get(i), "r" + i);
        }
        Assert.assertEquals(4 * INITIAL_CAPACITY, capacity(visited));

        visited.clear();
        Assert.assertNull("a grown table is dropped", visited.table);
        Assert.assertTrue(visited.isEmpty());
        for (int i = 0; i < 12; i++) {
            assertAbsent(visited, firsts.get(i), seconds.get(i));
        }

        visited.add(firsts.get(0), seconds.get(0), "again");
        Assert.assertEquals(INITIAL_CAPACITY, capacity(visited));
        Assert.assertEquals(1, occupied(visited));
        assertRecorded(visited, firsts.get(0), seconds.get(0), "again");
        for (int i = 1; i < 12; i++) {
            assertAbsent(visited, firsts.get(i), seconds.get(i));
        }

        // Clearing twice in a row, with the second clear on the initial-size table, is fine too.
        visited.clear();
        visited.clear();
        Assert.assertTrue(visited.isEmpty());
        assertAbsent(visited, firsts.get(0), seconds.get(0));
    }

    /** {@link EquivalentAtmComboScanner#visit} starts every scan with an empty history. */
    @Test
    public void visitClearsTheHistory() {
        TestScanner scanner = new TestScanner();
        AnnotatedTypeMirror a = newType();
        AnnotatedTypeMirror b = newType();
        AnnotatedTypeMirror c = newType();
        AnnotatedTypeMirror d = newType();

        scanner.visit(a, b, null);
        Assert.assertTrue(scanner.history().contains(a, b));
        Assert.assertFalse(scanner.history().contains(b, a));

        scanner.visit(c, d, null);
        Assert.assertTrue(scanner.history().contains(c, d));
        Assert.assertFalse(scanner.history().contains(a, b));
    }

    /**
     * The history that {@code Visited} replaced: a map from the first type to a map from the second
     * type to the result, both keyed on identity.
     */
    private static final class NestedMapHistory {

        /** The backing history of type pairs. */
        private IdentityHashMap<AnnotatedTypeMirror, IdentityHashMap<AnnotatedTypeMirror, String>>
                visits = new IdentityHashMap<>();

        /** Creates an empty history. */
        NestedMapHistory() {}

        /**
         * Returns true if no pairs have been recorded.
         *
         * @return true if no pairs have been recorded
         */
        boolean isEmpty() {
            return visits.isEmpty();
        }

        /** Clears the history. */
        void clear() {
            visits = new IdentityHashMap<>();
        }

        /**
         * Returns true if the given pair has been recorded.
         *
         * @param type1 the first type
         * @param type2 the second type
         * @return true if the given pair has been recorded
         */
        boolean contains(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
            IdentityHashMap<AnnotatedTypeMirror, String> recordFor1 = visits.get(type1);
            return recordFor1 != null && recordFor1.containsKey(type2);
        }

        /**
         * Returns the result recorded for the given pair, or null if none was recorded.
         *
         * @param type1 the first type
         * @param type2 the second type
         * @return the result recorded for the given pair, or null if none was recorded
         */
        @Nullable String getResult(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
            IdentityHashMap<AnnotatedTypeMirror, String> recordFor1 = visits.get(type1);
            return recordFor1 == null ? null : recordFor1.get(type2);
        }

        /**
         * Adds a pair to the history.
         *
         * @param type1 the first type
         * @param type2 the second type
         * @param ret the result
         */
        void add(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, @Nullable String ret) {
            visits.computeIfAbsent(type1, __ -> new IdentityHashMap<>()).put(type2, ret);
        }
    }

    /**
     * Over a seeded sequence of adds, lookups and clears on a small pool of types, the table
     * answers every query exactly as the nested identity maps did, after every operation.
     */
    @Test
    public void agreesWithNestedIdentityMaps() {
        EquivalentAtmComboScanner<String, Void>.Visited visited = SCANNER.newVisited();
        NestedMapHistory model = new NestedMapHistory();
        int poolSize = 12;
        List<AnnotatedTypeMirror> pool = new ArrayList<>();
        for (int i = 0; i < poolSize; i++) {
            pool.add(newType());
        }
        @Nullable String[] results = {"r0", "r1", "r2", null};
        Random random = new Random(2602);
        int maxCapacity = 0;
        int clearsOfGrownTable = 0;
        int clearsOfInitialTable = 0;
        for (int op = 0; op < 3000; op++) {
            int kind = random.nextInt(40);
            if (kind == 0) {
                boolean grown = visited.table != null && capacity(visited) > INITIAL_CAPACITY;
                visited.clear();
                model.clear();
                if (grown) {
                    clearsOfGrownTable++;
                } else {
                    clearsOfInitialTable++;
                }
            } else {
                AnnotatedTypeMirror type1 = pool.get(random.nextInt(poolSize));
                AnnotatedTypeMirror type2 = pool.get(random.nextInt(poolSize));
                if (kind < 20) {
                    @Nullable String result = results[random.nextInt(results.length)];
                    visited.add(type1, type2, result);
                    model.add(type1, type2, result);
                } else if (kind < 30) {
                    Assert.assertEquals(
                            model.contains(type1, type2), visited.contains(type1, type2));
                } else {
                    Assert.assertEquals(
                            model.getResult(type1, type2), visited.getResult(type1, type2));
                }
            }
            Assert.assertEquals(model.isEmpty(), visited.isEmpty());
            if (visited.table != null) {
                maxCapacity = Math.max(maxCapacity, capacity(visited));
            }
            for (AnnotatedTypeMirror type1 : pool) {
                for (AnnotatedTypeMirror type2 : pool) {
                    Assert.assertEquals(
                            model.contains(type1, type2), visited.contains(type1, type2));
                    Assert.assertEquals(
                            model.getResult(type1, type2), visited.getResult(type1, type2));
                }
            }
        }
        // The sequence is fixed by the seed, so it reaches these boundaries every time.
        Assert.assertTrue("grew four times: " + maxCapacity, maxCapacity >= 16 * INITIAL_CAPACITY);
        Assert.assertTrue("cleared a grown table", clearsOfGrownTable > 0);
        Assert.assertTrue("cleared an initial-size table", clearsOfInitialTable > 0);
    }
}
