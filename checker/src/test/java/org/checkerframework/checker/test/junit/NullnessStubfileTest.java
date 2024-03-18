package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class NullnessStubfileTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessStubfileTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessStubfileTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Astubs="
                        + String.join(
                                File.pathSeparator,
                                ("tests/nullness-stubfile/stubfile1.astub")
                                        .replace("/", File.separator),
                                ("tests/nullness-stubfile/stubfile2.astub")
                                        .replace("/", File.separator),
                                ("tests/nullness-stubfile/requireNonNull.astub")
                                        .replace("/", File.separator)));
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-stubfile"};
    }
}
