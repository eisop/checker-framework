package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Disabled regression test for per-bound annotation preservation on intersection-type upper bounds.
 *
 * <p>An intersection-type bound such as {@code <T extends @Odd IfaceA & IfaceB>} homogenizes its
 * per-bound annotations: {@code AnnotatedIntersectionType.copyIntersectionBoundAnnotations} feeds
 * the summarized primary annotation through {@code addAnnotations}, whose intersection override
 * calls {@code fixupBoundAnnotations} and copies that primary back onto every bound. The
 * unannotated bound therefore loses its default (top) qualifier and appears to carry the other
 * bound's qualifier, order-dependently. The test inputs in {@code
 * tests/disabled-intersection-bounds} assert the fixed, per-bound behavior.
 *
 * <p>This test is {@link Ignore}d because the narrowest fix (not writing the primary back onto the
 * bounds) changes intersection-bound defaulting and subtyping semantics that the standard Nullness
 * Checker tests deliberately rely on (see {@code checker/tests/nullness/Issue868.java} and {@code
 * Issue3349.java}), so it is out of scope for a contained bug fix. See {@code
 * cf-tasks/task-2-findings.md} for the full diagnosis and proposed patch.
 */
@Ignore("Fix changes intersection-bound semantics relied on by the Nullness Checker; see findings.")
public class DisabledIntersectionBoundAnnosTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a new DisabledIntersectionBoundAnnosTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public DisabledIntersectionBoundAnnosTest(List<File> testFiles) {
        super(testFiles, EvenOddChecker.class, "disabled-intersection-bounds");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"disabled-intersection-bounds"};
    }
}
