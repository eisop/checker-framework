package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.h1h2checker.H1H2Checker;
import org.junit.Ignore;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Disabled regression test for per-element annotation preservation on intersection-type bounds,
 * using a checker with two qualifier hierarchies.
 *
 * <p>With two independent hierarchies, the homogenization performed by {@code
 * AnnotatedIntersectionType} is visible even without conflicting annotations: for {@code <T
 * extends @H1S1 IfaceA & @H2S1 IfaceB>} the intersection's primary annotations collect
 * {@code @H1S1} and {@code @H2S1} and are copied onto both bounds, so each bound appears to carry
 * the other bound's qualifier instead of its own default. The test inputs in {@code
 * tests/disabled-intersection-multihierarchy} assert the per-element behavior.
 *
 * <p>This test is {@link Ignore}d because per-element intersection-bound semantics are not
 * implemented; adopting them is a semantics decision that also affects the Nullness Checker (see
 * {@code checker/tests/nullness/Issue868.java}). See {@code cf-tasks2/task-2-findings.md} for the
 * migration analysis.
 */
@Ignore("Encodes per-element intersection-bound semantics that are not implemented; see findings.")
public class DisabledIntersectionMultiHierarchyTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a new DisabledIntersectionMultiHierarchyTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public DisabledIntersectionMultiHierarchyTest(List<File> testFiles) {
        super(testFiles, H1H2Checker.class, "disabled-intersection-multihierarchy");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"disabled-intersection-multihierarchy"};
    }
}
