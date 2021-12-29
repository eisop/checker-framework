package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collections;
import java.util.List;

/** JUnit tests for the Nullness checker. */
public class NullnessNullMarkedSubpackageTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessAssertsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessNullMarkedSubpackageTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                Collections.singletonList("../../jspecify/build/libs/jspecify-0.0.0-SNAPSHOT.jar"),
                "-AcheckPurityAnnotations",
                "-AassumeAssertionsAreEnabled",
                "-Anomsgtext",
                "-Xlint:deprecation");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-nullmarked-subpackage"};
    }
}
