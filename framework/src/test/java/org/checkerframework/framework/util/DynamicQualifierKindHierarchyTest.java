package org.checkerframework.framework.util;

import org.checkerframework.framework.qual.ReadWriteDynamicQualifier;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;
import org.checkerframework.javacutil.TypeSystemError;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Tests that a read-write dynamic qualifier is placed in the hierarchy named by {@link
 * ReadWriteDynamicQualifier#value()}, rather than in an arbitrary one.
 *
 * <p>These tests use a type system with two hierarchies, so that choosing the wrong one is
 * detectable. The qualifiers come from the h1h2 test checker, but the hierarchy under test is built
 * directly, so the h1h2 checker itself is unaffected.
 */
public class DynamicQualifierKindHierarchyTest {

    /** A read-write dynamic qualifier that names a non-top qualifier of the second hierarchy. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @ReadWriteDynamicQualifier(H2S1.class)
    @interface DynViaH2S1 {}

    /** A read-write dynamic qualifier that names the top of the second hierarchy directly. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @ReadWriteDynamicQualifier(H2Top.class)
    @interface DynViaH2Top {}

    /** A read-write dynamic qualifier that names a non-top qualifier of the first hierarchy. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @ReadWriteDynamicQualifier(H1S1.class)
    @interface DynViaH1S1 {}

    /** A read-write dynamic qualifier that names a qualifier outside the type system. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @ReadWriteDynamicQualifier(Deprecated.class)
    @interface DynViaUnsupported {}

    /**
     * Returns the two-hierarchy qualifier set, plus {@code dynamic}.
     *
     * @param dynamic the read-write dynamic qualifier to include
     * @return the qualifier classes for the hierarchy under test
     */
    private Collection<Class<? extends Annotation>> qualsWith(Class<? extends Annotation> dynamic) {
        List<Class<? extends Annotation>> quals =
                new ArrayList<>(Arrays.asList(H1Top.class, H1S1.class, H2Top.class, H2S1.class));
        quals.add(dynamic);
        return quals;
    }

    /**
     * Returns the qualifier kind for {@code clazz} in {@code hierarchy}.
     *
     * @param hierarchy a qualifier kind hierarchy
     * @param clazz an annotation class
     * @return the qualifier kind for {@code clazz}
     */
    private QualifierKind kindOf(QualifierKindHierarchy hierarchy, Class<?> clazz) {
        return hierarchy.getQualifierKind(clazz.getCanonicalName());
    }

    @Test
    public void dynamicViaNonTopUsesThatHierarchysTop() {
        QualifierKindHierarchy hierarchy =
                new DefaultQualifierKindHierarchy(qualsWith(DynViaH2S1.class));
        QualifierKind dyn = kindOf(hierarchy, DynViaH2S1.class);
        Assert.assertTrue("should be recognized as dynamic", dyn.isDynamicAnnotation());
        Assert.assertEquals(
                "dynamic qualifier must land in H2, the hierarchy of its value()",
                kindOf(hierarchy, H2Top.class),
                dyn.getTop());
    }

    @Test
    public void dynamicViaTopUsesThatTop() {
        QualifierKindHierarchy hierarchy =
                new DefaultQualifierKindHierarchy(qualsWith(DynViaH2Top.class));
        Assert.assertEquals(
                kindOf(hierarchy, H2Top.class), kindOf(hierarchy, DynViaH2Top.class).getTop());
    }

    @Test
    public void dynamicViaOtherHierarchyUsesOtherTop() {
        // The mirror image of dynamicViaNonTopUsesThatHierarchysTop: whichever top a buggy
        // implementation picks arbitrarily, one of these two tests fails.
        QualifierKindHierarchy hierarchy =
                new DefaultQualifierKindHierarchy(qualsWith(DynViaH1S1.class));
        Assert.assertEquals(
                kindOf(hierarchy, H1Top.class), kindOf(hierarchy, DynViaH1S1.class).getTop());
    }

    @Test
    public void dynamicViaUnsupportedQualifierIsRejected() {
        TypeSystemError e =
                Assert.assertThrows(
                        TypeSystemError.class,
                        () ->
                                new DefaultQualifierKindHierarchy(
                                        qualsWith(DynViaUnsupported.class)));
        Assert.assertTrue(
                "message should name the unsupported qualifier, but was: " + e.getMessage(),
                e.getMessage().contains("Deprecated"));
    }
}
