package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit tests for the Initialization Checker with the type caches of the type factory turned off
 * ({@code -AatfDoNotCache}). The option must not change any diagnostic; the test directories are
 * the ones of {@link NullnessTest} that exercise initialization, and the expected diagnostics are
 * the same files. The Initialization Checker computes the type of {@code this} from the dataflow
 * store, so it is the checker most sensitive to a type cache that is used at the wrong time (see
 * eisop issues 2344 and 2354).
 */
public class NullnessInitializationDoNotCacheTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessInitializationDoNotCacheTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessInitializationDoNotCacheTest(List<File> testFiles) {
        super(
                testFiles,
                NullnessChecker.class,
                "nullness",
                "-AatfDoNotCache",
                "-AcheckPurityAnnotations",
                "-AconservativeArgumentNullnessAfterInvocation=true",
                "-Xlint:deprecation",
                "-Alint=soundArrayCreationNullness,"
                        + NullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-initialization", "initialization"};
    }
}
