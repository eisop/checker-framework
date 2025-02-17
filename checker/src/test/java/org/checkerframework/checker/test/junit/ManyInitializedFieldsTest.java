package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JUnit tests for multiple checkers, in particular the Nullness Checker, when running together with
 * the InitializedFields Checker.
 */
public class ManyInitializedFieldsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an ManyInitializedFieldsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public ManyInitializedFieldsTest(List<File> testFiles) {
        super(
                testFiles,
                Arrays.asList(
                        "org.checkerframework.checker.formatter.FormatterChecker",
                        "org.checkerframework.checker.index.IndexChecker",
                        "org.checkerframework.checker.interning.InterningChecker",
                        "org.checkerframework.checker.lock.LockChecker",
                        "org.checkerframework.checker.nullness.NullnessChecker",
                        "org.checkerframework.checker.regex.RegexChecker",
                        "org.checkerframework.checker.resourceleak.ResourceLeakChecker",
                        "org.checkerframework.checker.signature.SignatureChecker",
                        "org.checkerframework.checker.signedness.SignednessChecker",
                        "org.checkerframework.common.initializedfields.InitializedFieldsChecker"),
                "many-initializedfields",
                Collections.emptyList());
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {
            "many-initializedfields",
            // TODO: remove this once a minimal reproduction is in the above directory.
            "index-initializedfields"
        };
    }
}
