package org.checkerframework.framework.test.junit;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Options;

import org.checkerframework.framework.testchecker.util.Encrypted;
import org.checkerframework.framework.testchecker.util.Odd;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.SubtypeVisitHistory;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.NavigableSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

/**
 * Regression tests for performance-relevant invariants that have broken in the past or whose
 * accidental reintroduction would silently undo a shipped optimization.
 *
 * <p>Every test in this class documents the PR it guards (or the rejected-direction it prevents),
 * so a future reader can decide whether their proposed change is actually safe. Do not delete a
 * test here without understanding what it protects; the corresponding entry in {@code
 * docs/developer/performance-notes.md} has the full rationale.
 */
public class PerfRegressionTest {

    /** The processing environment to use. */
    private final ProcessingEnvironment env;

    /** Default constructor. */
    public PerfRegressionTest() {
        Context context = new Context();
        Options options = Options.instance(context);
        options.put(Option.SOURCE, "8");
        options.put(Option.TARGET, "8");
        env = JavacProcessingEnvironment.instance(context);
        JavaCompiler javac = JavaCompiler.instance(context);
        javac.initModules(List.nil());
        javac.enterDone();
    }

    /**
     * Build an AnnotationMirror for the given class.
     *
     * @param c the class of the annotation
     * @return the AnnotationMirror corresponding to class c
     */
    private AnnotationMirror anno(Class<? extends java.lang.annotation.Annotation> c) {
        return new AnnotationBuilder(env, c).build();
    }

    // ---------------------------------------------------------------------
    // AnnotationMirrorSet
    // ---------------------------------------------------------------------

    /**
     * {@link AnnotationMirrorSet#addAll} must return {@code true} when any element was newly added,
     * per the standard {@link java.util.Set#addAll} contract. PR #1649 initially shipped with this
     * collapsed to an all-or-nothing return value, which broke {@code indextest} where callers rely
     * on {@code addAll} to report whether the receiver grew at all.
     */
    @Test
    public void annotationMirrorSet_addAll_returnsTrueIfAnyNew() {
        AnnotationMirror a = anno(Encrypted.class);
        AnnotationMirror b = anno(Odd.class);

        AnnotationMirrorSet target = new AnnotationMirrorSet();
        target.add(a);

        AnnotationMirrorSet other = new AnnotationMirrorSet();
        other.add(a); // already in target
        other.add(b); // new

        Assert.assertTrue(
                "addAll must return true when any element is new (PR #1649 regression)",
                target.addAll(other));
        Assert.assertEquals(2, target.size());
    }

    /** Complementary case: addAll of an all-duplicate collection must return false. */
    @Test
    public void annotationMirrorSet_addAll_returnsFalseIfAllDuplicates() {
        AnnotationMirror a = anno(Encrypted.class);
        AnnotationMirror b = anno(Odd.class);

        AnnotationMirrorSet target = new AnnotationMirrorSet();
        target.add(a);
        target.add(b);

        AnnotationMirrorSet other = new AnnotationMirrorSet();
        other.add(a);
        other.add(b);

        Assert.assertFalse("addAll must return false when no element is new", target.addAll(other));
        Assert.assertEquals(2, target.size());
    }

    /**
     * The bare {@link AnnotationMirrorSet#iterator()} is mutable. The unmodifiable wrapper comes
     * from {@link AnnotationMirrorSet#unmodifiableSet} (formerly {@code makeUnmodifiable}), not
     * from {@code iterator()} itself. Past patches have inverted this; the cost is a defensive copy
     * on every iteration.
     */
    @Test
    public void annotationMirrorSet_iterator_isMutableByDefault() {
        AnnotationMirrorSet s = new AnnotationMirrorSet();
        s.add(anno(Encrypted.class));
        s.add(anno(Odd.class));

        Iterator<AnnotationMirror> it = s.iterator();
        it.next();
        it.remove(); // must not throw on a non-unmodifiable view
        Assert.assertEquals(1, s.size());
    }

    /**
     * {@link AnnotationMirrorSet} must not implement {@link NavigableSet}. PR #1649 dropped the
     * {@code TreeSet} backing in favor of {@code ArrayList}; the changelog promises {@code Set}-
     * only semantics. Re-implementing {@code NavigableSet} would reintroduce the {@code
     * compareTo}-driven {@code Name.toString} decoding that prompted the rewrite.
     */
    @Test
    public void annotationMirrorSet_isNotNavigableSet() {
        Assert.assertFalse(
                "AnnotationMirrorSet must not implement NavigableSet (PR #1649; see CHANGELOG)",
                NavigableSet.class.isAssignableFrom(AnnotationMirrorSet.class));
    }

    // ---------------------------------------------------------------------
    // AnnotationUtils
    // ---------------------------------------------------------------------

    /**
     * After PR #1673, {@link AnnotationUtils#annotationName} returns an interned string. The
     * separate {@code annotationNameInterned} method was removed because it was redundant. Hot
     * downstream callers (qualifier hierarchy lookups, supported-qualifier checks) rely on this: a
     * regression here would silently re-allocate one {@code String} per call.
     */
    @Test
    public void annotationUtils_annotationName_returnsInternedString() {
        AnnotationMirror a1 = anno(Encrypted.class);
        AnnotationMirror a2 = anno(Encrypted.class);

        String n1 = AnnotationUtils.annotationName(a1);
        String n2 = AnnotationUtils.annotationName(a2);

        Assert.assertEquals(n1, n2);
        Assert.assertSame(
                "AnnotationUtils.annotationName must return an interned String (PR #1673)",
                n1.intern(),
                n1);
    }

    /**
     * The {@link AnnotationUtils#annotationNameAsName} accessor introduced in PR #1669 returns the
     * underlying {@code Name} without going through {@code toString}. Removing it would force the
     * hot LUB/GLB paths back to per-call UTF-8 decoding.
     */
    @Test
    public void annotationUtils_annotationNameAsName_exists() throws NoSuchMethodException {
        Method m = AnnotationUtils.class.getMethod("annotationNameAsName", AnnotationMirror.class);
        Assert.assertEquals(
                "annotationNameAsName must be public static (PR #1669)",
                Modifier.PUBLIC | Modifier.STATIC,
                m.getModifiers() & (Modifier.PUBLIC | Modifier.STATIC));
    }

    // ---------------------------------------------------------------------
    // AnnotatedTypeScanner
    // ---------------------------------------------------------------------

    /**
     * {@code AnnotatedTypeScanner.visitedNodes} must be an {@link IdentityHashMap}, not a plain
     * {@code HashMap}. Identity is required for correctness: distinct ATM instances representing
     * the same Java type must be visited separately so cycle-breaking works. It is also faster (no
     * {@code hashCode} dispatch). See {@code docs/developer/performance-notes.md}.
     */
    @Test
    public void annotatedTypeScanner_visitedNodes_isIdentityHashMap() throws NoSuchFieldException {
        Field f = AnnotatedTypeScanner.class.getDeclaredField("visitedNodes");
        Assert.assertEquals(
                "visitedNodes must be declared as IdentityHashMap (tried-and-rejected: HashMap)",
                IdentityHashMap.class,
                f.getType());
    }

    // ---------------------------------------------------------------------
    // AnnotatedTypeFactory
    // ---------------------------------------------------------------------

    /**
     * {@code AnnotatedTypeFactory.annotationClassNames} must not be wrapped in {@code
     * Collections.synchronizedMap}. The wrapper was carried over from a 2020 refactor of a
     * previously-static cache; the threading audit showed AT factories are confined to the javac
     * main thread, matching every other per-factory LRU on the same object. Reintroducing the
     * wrapper would re-add the per-call monitor enter/exit cost.
     *
     * <p>The check inspects the declared field type. {@code Collections.synchronizedMap} returns an
     * instance of {@code Collections$SynchronizedMap}; a regression that reintroduces
     * synchronization would typically widen the declared type back to {@code Map}, and this test
     * will fail then.
     */
    @Test
    public void annotatedTypeFactory_annotationClassNames_notSynchronized() throws Exception {
        Field f = AnnotatedTypeFactory.class.getDeclaredField("annotationClassNames");
        // The field is declared as IdentityHashMap, not Map, since the synchronizedMap wrapper
        // was removed. If a future refactor changes the static type back to Map, that may be a
        // signal that someone is preparing to re-wrap; review then.
        Assert.assertEquals(
                "annotationClassNames must be declared as IdentityHashMap (tried-and-rejected:"
                        + " synchronizedMap wrapper)",
                IdentityHashMap.class,
                f.getType());
    }

    // ---------------------------------------------------------------------
    // SubtypeVisitHistory / StructuralEqualityVisitHistory
    // ---------------------------------------------------------------------

    /**
     * The package-private {@code putKey}/{@code removeKey}/{@code containsKey} methods on {@link
     * SubtypeVisitHistory} exist to let {@code StructuralEqualityVisitHistory} share one {@code
     * IPair} across both inner histories per public call. Removing them would halve the allocation
     * reduction shipped in PR #1719.
     */
    @Test
    public void subtypeVisitHistory_keyedOverloads_exist() {
        long count =
                Arrays.stream(SubtypeVisitHistory.class.getDeclaredMethods())
                        .filter(m -> m.getName().endsWith("Key"))
                        .filter(m -> !Modifier.isPublic(m.getModifiers()))
                        .filter(m -> !Modifier.isPrivate(m.getModifiers()))
                        .count();
        Assert.assertEquals(
                "SubtypeVisitHistory must expose putKey, removeKey, containsKey as"
                        + " package-private overloads (PR #1719)",
                3L,
                count);
    }
}
